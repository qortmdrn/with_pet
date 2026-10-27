#!/usr/bin/env python3
"""안정 사운드 임시 음원 합성 (저작권 걱정 없는 자체 제작).

사용: python3 scripts/generate_calm_sounds.py app/src/main/res/raw

- 곡마다 60초, 끝과 시작을 겹쳐(crossfade) 반복 재생해도 끊김이 없다 (CalmPlaybackService는 REPEAT_MODE_ALL).
- 편안한 차량 내 청각 환경을 위한 원칙: 느린 템포, 중저음 위주, 갑작스러운 소리 없음, 고음(약 4kHz 이상) 감쇠.
  멀미·불안 완화 같은 효과를 노리거나 주장하지 않는다 (기획서 7-5).
- WAV로 합성한 뒤 afconvert(macOS) 또는 ffmpeg로 AAC(.m4a, 모노 64kbps)로 변환한다.
- 출시용 음원이 정해지면 교체한다 (기획서 7-5).
"""
import os
import shutil
import subprocess
import sys
import tempfile
import wave

import numpy as np

SR = 44100
LENGTH_S = 60
XFADE_S = 4
RNG = np.random.default_rng(20261023)  # 다시 만들어도 같은 소리


# --- 기본 재료 ---------------------------------------------------------------

def t_axis(seconds):
    return np.arange(int(seconds * SR)) / SR


def shape(signal, gain_of_freq):
    """주파수 영역에서 [gain_of_freq]를 곱한다 (필터)."""
    spec = np.fft.rfft(signal)
    freqs = np.fft.rfftfreq(len(signal), 1 / SR)
    spec *= gain_of_freq(np.maximum(freqs, 1.0))
    return np.fft.irfft(spec, len(signal))


def lowpass(signal, cutoff):
    return shape(signal, lambda f: 1 / np.sqrt(1 + (f / cutoff) ** 4))


def highpass(signal, cutoff):
    return shape(signal, lambda f: 1 / np.sqrt(1 + (cutoff / f) ** 4))


def normalize(x):
    return x / (np.max(np.abs(x)) + 1e-9)


def brown_noise(seconds):
    return normalize(highpass(shape(RNG.standard_normal(int(seconds * SR)), lambda f: 1 / f), 20))


def pink_noise(seconds):
    return normalize(highpass(shape(RNG.standard_normal(int(seconds * SR)), lambda f: 1 / np.sqrt(f)), 20))


def midi_hz(note):
    return 440.0 * 2 ** ((note - 69) / 12)


def envelope(n, attack, release):
    """천천히 올라가고 천천히 내려가는 포락선 (raised cosine)."""
    env = np.ones(n)
    a, r = int(attack * SR), int(release * SR)
    if a:
        env[:a] = 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, a))
    if r:
        env[-r:] *= 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, r))
    return env


def pad_note(note, seconds, attack=3.0, release=3.0):
    """부드러운 패드: 기음 + 약한 배음, 살짝 어긋난 두 음으로 따뜻하게."""
    t = t_axis(seconds)
    f = midi_hz(note)
    tone = sum(
        amp * (np.sin(2 * np.pi * f * k * t) + np.sin(2 * np.pi * f * k * 1.003 * t + 1.0))
        for k, amp in [(1, 1.0), (2, 0.25), (3, 0.08)]
    )
    return tone * envelope(len(t), attack, release)


def soft_bell(note, seconds=4.0):
    """오르골 같은 부드러운 음. 공격을 느리게 해 '딩' 소리가 튀지 않게 한다."""
    t = t_axis(seconds)
    f = midi_hz(note)
    tone = np.sin(2 * np.pi * f * t) + 0.15 * np.sin(2 * np.pi * 2 * f * t)
    env = np.minimum(t / 0.08, 1.0) * np.exp(-t / 1.4)
    return tone * env


def heartbeat(seconds, bpm):
    """낮고 부드러운 심장 박동(쿵-쿵)."""
    out = np.zeros(int(seconds * SR))
    beat = t_axis(0.25)
    # 차량 스피커가 낼 수 있는 높이(70Hz 이상)로, 배음을 더해 작은 스피커에서도 들리게 한다
    thump = (np.sin(2 * np.pi * 75 * beat) + 0.35 * np.sin(2 * np.pi * 150 * beat)) * np.sin(np.pi * beat / 0.25) ** 2
    for start in np.arange(0, seconds, 60 / bpm):
        for offset, amp in [(0.0, 1.0), (0.28, 0.6)]:
            i = int((start + offset) * SR)
            j = min(i + len(thump), len(out))
            out[i:j] += amp * thump[: j - i]
    return out


def place(track, sound, at):
    i = int(at * SR)
    j = min(i + len(sound), len(track))
    track[i:j] += sound[: j - i]


def chords(seconds, progression, chord_len, octave_shift=0, attack=3.0, release=3.0):
    """chord_len초마다 화음을 바꾸고, 이전 화음과 겹치게 해서 이음새가 없게 한다."""
    out = np.zeros(int(seconds * SR))
    for i, start in enumerate(np.arange(-chord_len, seconds, chord_len)):
        for note in progression[i % len(progression)]:
            sound = pad_note(note + octave_shift, chord_len + release, attack, release)
            if start < 0:
                sound = sound[int(-start * SR):]
                start = 0
            place(out, sound, start)
    return normalize(out)


def swell(seconds, period_s, depth):
    """period_s 주기로 천천히 커졌다 작아지는 볼륨 (호흡·파도)."""
    t = t_axis(seconds)
    return 1 - depth * (0.5 + 0.5 * np.cos(2 * np.pi * t / period_s))


# --- 곡 ----------------------------------------------------------------------
# 화음은 MIDI 번호 (60 = 가온 도)

C, Am, F, G = [48, 55, 60, 64], [45, 52, 57, 60], [41, 48, 53, 57], [43, 50, 55, 59]
Dm, Em = [50, 57, 62, 65], [40, 47, 52, 55]
TOTAL = LENGTH_S + XFADE_S


def departure_calm():  # 잔잔한 출발: 밝은 장조 패드, 8초마다 화음 변화
    return chords(TOTAL, [C, Am, F, G], 8.0)


def departure_breeze():  # 부드러운 바람: 핑크 노이즈 바람 + 옅은 패드
    wind = lowpass(pink_noise(TOTAL), 900) * swell(TOTAL, 12.0, 0.6)
    return 0.8 * normalize(wind) + 0.25 * chords(TOTAL, [F, C], 16.0)


def long_waves():  # 느린 파도: 브라운 노이즈가 10초 주기로 밀려왔다 빠진다
    return normalize(lowpass(brown_noise(TOTAL), 1200) * swell(TOTAL, 10.0, 0.75))


def long_forest():  # 숲속 오후: 나뭇잎 스치는 소리 + 낮은 지속음
    rustle = lowpass(highpass(pink_noise(TOTAL), 300), 2500) * swell(TOTAL, 7.0, 0.4)
    return 0.6 * normalize(rustle) + 0.4 * chords(TOTAL, [[43, 50, 55], [41, 48, 53]], 15.0)


def long_field():  # 고요한 들판: 5도 화음 위주의 아주 느린 드론
    return chords(TOTAL, [[36, 43, 48], [38, 45, 50], [34, 41, 46], [36, 43, 48]], 16.0, attack=6.0, release=6.0)


def hospital_okay():  # 괜찮아: 분당 60회 부드러운 심장 박동 + 낮은 패드
    return 0.55 * normalize(heartbeat(TOTAL, 60)) + 0.45 * chords(TOTAL, [Am, F, C, G], 10.0, octave_shift=-12)


def hospital_breathe():  # 천천히 숨쉬기: 10초 주기(분당 6회) 호흡에 맞춰 커졌다 작아진다
    breath = swell(TOTAL, 10.0, 0.85)
    air = lowpass(brown_noise(TOTAL), 900)
    return normalize(0.7 * normalize(air) * breath + 0.3 * chords(TOTAL, [Dm, Am], 20.0, octave_shift=-12) * breath)


def home_evening():  # 포근한 저녁: 따뜻한 패드 위로 느린 오르골 음 (분당 30음)
    out = 0.6 * chords(TOTAL, [F, C, Am, G], 8.0, octave_shift=-12)
    melody = [72, 69, 67, 64, 67, 69, 67, 64, 62, 64, 60, 62]  # C 장조 5음계
    for i, at in enumerate(np.arange(0.5, TOTAL - 4, 2.0)):
        place(out, 0.25 * soft_bell(melody[i % len(melody)]), at)
    return normalize(out)


def home_blanket():  # 따뜻한 담요: 아주 낮은 브라운 노이즈 + 저음 패드
    hum = lowpass(brown_noise(TOTAL), 600)
    return 0.6 * normalize(hum) + 0.4 * chords(TOTAL, [[41, 48, 57], [36, 43, 52]], 15.0, octave_shift=-12)


TRACKS = {
    "calm_departure_calm": departure_calm,
    "calm_departure_breeze": departure_breeze,
    "calm_long_waves": long_waves,
    "calm_long_forest": long_forest,
    "calm_long_field": long_field,
    "calm_hospital_okay": hospital_okay,
    "calm_hospital_breathe": hospital_breathe,
    "calm_home_evening": home_evening,
    "calm_home_blanket": home_blanket,
}


# --- 마무리·저장 --------------------------------------------------------------

def finish(x):
    """고음 감쇠·초저음 제거 → 끝과 시작을 겹쳐 루프 → 음량 맞추기.

    70Hz 아래는 차량 스피커가 거의 못 내고 노면 소음에 묻힌다. 그 대역에 에너지를 두면 음량을 맞춰도 조용하게 들린다.
    """
    x = highpass(lowpass(x, 4000), 70)
    n, xf = LENGTH_S * SR, XFADE_S * SR
    fade = np.linspace(0, 1, xf)
    looped = x[:n].copy()
    # 마지막 XFADE_S초(n~n+xf)를 처음에 겹쳐 넣는다
    looped[:xf] = x[:xf] * fade + x[n:n + xf] * (1 - fade)
    rms = np.sqrt(np.mean(looped ** 2))
    looped *= 0.12 / rms  # 약 -18 dBFS RMS: 차량 스피커에서 은은하게
    return np.clip(looped, -0.9, 0.9)


def write_wav(path, x):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((x * 32767).astype("<i2").tobytes())


def encode(wav, out):
    if shutil.which("afconvert"):
        subprocess.run(["afconvert", "-f", "m4af", "-d", "aac", "-b", "64000", wav, out], check=True)
    elif shutil.which("ffmpeg"):
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", wav, "-c:a", "aac", "-b:a", "64k", out], check=True)
    else:
        sys.exit("afconvert 또는 ffmpeg가 필요합니다")


def main(out_dir):
    with tempfile.TemporaryDirectory() as tmp:
        for name, make in TRACKS.items():
            wav = os.path.join(tmp, f"{name}.wav")
            write_wav(wav, finish(make()))
            out = os.path.join(out_dir, f"{name}.m4a")
            encode(wav, out)
            print(f"{out} ({os.path.getsize(out) // 1024}KB)")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])

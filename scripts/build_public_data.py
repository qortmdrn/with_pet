#!/usr/bin/env python3
"""반려동물 동반 가능 문화시설 CSV → 앱 asset(JSON) 변환.

사용: python3 scripts/build_public_data.py data/facilities_with_pet.csv app/src/main/assets/data

출력
- pet_places.json: 카페·식당 / 여행지 / 문화시설(박물관·미술관·문예회관) 중 '반려동물 동반 가능정보 = Y'
- hospitals.json: 동물병원 (응급 모드)

- 운영시간·휴무일은 원문 그대로 둔다. 해석은 앱의 OpeningHoursParser가 한다.
- 같은 시설(이름+주소)이 여러 번 있으면 최종작성일이 가장 최근인 것만 남긴다.
"""
import csv
import hashlib
import json
import sys

CATEGORY = {
    "카페": "CAFE",
    "식당": "CAFE",
    "여행지": "TRAVEL",
    "박물관": "CULTURE",
    "미술관": "CULTURE",
    "문예회관": "CULTURE",
}
# 의미 없는 값은 null로
EMPTY = {"", "정보없음", "해당없음", "제한사항 없음", "없음"}
# 대한민국 범위 밖 좌표는 버린다
LAT_RANGE = (33.0, 39.0)
LNG_RANGE = (124.0, 132.0)


def value(raw):
    raw = (raw or "").strip()
    return None if raw in EMPTY else raw


def location(r):
    """대한민국 범위 안의 좌표만. 아니면 None."""
    try:
        lat, lng = float(r["위도"]), float(r["경도"])
    except ValueError:
        return None
    if not (LAT_RANGE[0] <= lat <= LAT_RANGE[1] and LNG_RANGE[0] <= lng <= LNG_RANGE[1]):
        return None
    return round(lat, 6), round(lng, 6)


def common(r, lat, lng):
    name = r["시설명"].strip()
    address = value(r["도로명주소"]) or value(r["지번주소"]) or ""
    return {
        "id": hashlib.sha1(f"{name}|{address}".encode()).hexdigest()[:12],
        "name": name,
        "lat": lat,
        "lng": lng,
        "address": address,
        "phone": value(r["전화번호"]),
        "hours": value(r["운영시간"]),
        "closedDays": value(r["휴무일"]),
        "updatedAt": r["최종작성일"].strip(),
    }


def keep_latest(items, item):
    key = (item["name"], item["address"])
    prev = items.get(key)
    if prev is None or item["updatedAt"] > prev["updatedAt"]:
        items[key] = item


def main(src, out_dir):
    places, hospitals = {}, {}
    with open(src, encoding="utf-8-sig") as f:
        for r in csv.DictReader(f):
            loc = location(r)
            if loc is None:
                continue
            if r["카테고리3"] == "동물병원":
                keep_latest(hospitals, common(r, *loc))
                continue
            category = CATEGORY.get(r["카테고리3"])
            # 약국 등 의료 시설은 동반 장소에 넣지 않는다
            if category is None or r["카테고리2"] == "반려의료" or r["반려동물 동반 가능정보"] != "Y":
                continue
            keep_latest(places, {
                **common(r, *loc),
                "category": category,
                "parking": r["주차 가능여부"] == "Y",
                "sizeLimit": value(r["입장 가능 동물 크기"]),
                "restriction": value(r["반려동물 제한사항"]),
                "extraFee": value(r["애견 동반 추가 요금"]),
            })

    write(f"{out_dir}/pet_places.json", sorted(places.values(), key=lambda p: (p["category"], p["name"], p["id"])))
    write(f"{out_dir}/hospitals.json", sorted(hospitals.values(), key=lambda p: (p["name"], p["id"])))


def write(path, items):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(items, f, ensure_ascii=False, separators=(",", ":"))
    counts = {}
    for item in items:
        counts[item.get("category", "-")] = counts.get(item.get("category", "-"), 0) + 1
    print(f"{len(items)}곳 → {path} {counts}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])

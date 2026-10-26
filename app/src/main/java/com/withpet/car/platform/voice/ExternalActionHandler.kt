package com.withpet.car.platform.voice

import android.content.Intent
import javax.inject.Inject

/** 다른 앱(Pleos Gleo AI 등)이 Intent로 보낸 명령을 처리한다. MainActivity가 받은 Intent를 넘긴다. */
interface ExternalActionHandler {
    /** 처리했으면 true */
    fun handle(intent: Intent): Boolean
}

/** 외부 명령을 받지 않는 플랫폼용 */
class NoExternalActions @Inject constructor() : ExternalActionHandler {
    override fun handle(intent: Intent) = false
}

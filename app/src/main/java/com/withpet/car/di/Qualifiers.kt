package com.withpet.car.di

import javax.inject.Qualifier

/** 앱 프로세스 수명 동안 유지되는 CoroutineScope */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

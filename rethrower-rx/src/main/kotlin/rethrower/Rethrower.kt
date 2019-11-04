package com.goozix.flipacar.extension

import com.goozix.flipacar.exception.BaseException
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Single
import io.reactivex.functions.Function

inline fun <T : Any> Single<T>.rethrowSingle(crossinline exception: (Throwable) -> BaseException): Single<T> =
    onErrorResumeNext { Single.error(exception(it)) }

inline fun <T : Any> Observable<T>.rethrowObservable(crossinline exception: (Throwable) -> BaseException): Observable<T> =
    onErrorResumeNext(Function<Throwable, ObservableSource<T>> {
        Observable.error<T>(exception(it))
    })

inline fun Completable.rethrowCompletable(crossinline exception: (Throwable) -> BaseException): Completable =
    onErrorResumeNext { Completable.error(exception(it)) }
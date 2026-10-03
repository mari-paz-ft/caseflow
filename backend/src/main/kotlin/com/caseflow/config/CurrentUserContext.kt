package com.caseflow.config

import com.caseflow.domain.model.AppUser

class CurrentUserContext {
    companion object {
        private val userThreadLocal = ThreadLocal<AppUser>()

        fun get(): AppUser? = userThreadLocal.get()
        fun set(user: AppUser) = userThreadLocal.set(user)
        fun clear() = userThreadLocal.remove()
    }
}

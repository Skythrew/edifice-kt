package io.github.skythrew.edificekt.resources

import io.ktor.resources.Resource

@Resource("/auth")
class Auth {
    @Resource("oauth2")
    class Oauth2 (val parent: Auth = Auth()) {
        @Resource("token")
        class Token (val parent: Oauth2 = Oauth2())

        @Resource("userinfo")
        class UserInfo (val parent: Oauth2 = Oauth2())
    }
}
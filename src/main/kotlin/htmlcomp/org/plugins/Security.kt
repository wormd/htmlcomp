package htmlcomp.org.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.ldap.*

fun Application.configureSecurity() {
    val localhost = "http://0.0.0.0"
    val ldapServerPort = 6998 // TODO: change to real value!
    authentication {
        basic("authName") {
            realm = "realm"
            validate { credential ->
                ldapAuthenticate(credential, "ldap://$localhost:${ldapServerPort}", "uid=%s,ou=system")
            }
        }
    }
}

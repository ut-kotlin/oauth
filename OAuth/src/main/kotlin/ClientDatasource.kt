import java.util.*

class ClientDatasource {
    private val clients = mapOf<UUID,String>(
        UUID.fromString("41515389-7EBE-466B-B532-394B7E9998D4")!! to "btENnDHAiXc8CgW54FXBtO3x9wnkEerepAl0vsim"
    )

    fun getSecret(clientId: UUID): String? {
        return clients.get(clientId)
    }
}
package de.p10r

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoDatabase
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import kotlin.test.assertEquals

data class Dealer(val id: String, val name: String)

// This is used by our core business logic
interface DealerRepository {
    fun save(dealer: Dealer)
    fun findAll(): List<Dealer>
}

// This is used in production and would be placed in src/main
class MongoDBRepository(client: MongoDatabase) : DealerRepository {
    private val db = client.getCollection<Dealer>("dealers")

    companion object {
        fun of(uri: String): MongoDBRepository {
            val settings = MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(uri))
                .retryWrites(true)
                .build()

            val db = MongoClient.create(settings).getDatabase("test")

            return MongoDBRepository(db)
        }
    }

    override fun save(dealer: Dealer) {
        db.insertOne(dealer)
    }

    override fun findAll(): List<Dealer> {
        return db.find().toList()
    }
}

// The in-memory fake
class FakeDealerRepository : DealerRepository {
    private val dealers = mutableMapOf<String, Dealer>()

    override fun save(dealer: Dealer) {
        dealers[dealer.id] = dealer
    }

    override fun findAll(): List<Dealer> = dealers.values.toList()
}

// This makes sure that the in-memory fake and the real DB are in sync
abstract class DBContract {
    abstract val repository: DealerRepository

    @Test
    fun `inserts and finds all`() {
        val dealer = Dealer(UUID.randomUUID().toString(), "Joe")

        repository.save(dealer)

        assertEquals(listOf(dealer), repository.findAll())
    }
}

class FakeDBTest : DBContract() {
    override val repository = FakeDealerRepository()
}

// Spins up a real MongoDB via testcontainers
class MongoDBTest : DBContract() {
    // we use lazy to make sure that only one testcontainer is booted,
    // since JUnit creates a new instance for each test
    override val repository by lazy { setupMongoDB() }

    private fun setupMongoDB(): MongoDBRepository {
        val dockerImage = DockerImageName.parse("mongo:4.0.10")
        val mongoDBContainer = MongoDBContainer(dockerImage).apply { start() }

        val uri = mongoDBContainer.firstMappedPort

        return MongoDBRepository.of("mongodb://localhost:$uri")
    }
}
package edu.nobel.data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import edu.nobel.domain.Laureate
import edu.nobel.domain.NobelPrize
import edu.nobel.domain.PrizeRepository
import edu.nobel.domain.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object Users : Table("users") {
    val id = integer("id")
    val username = varchar("username", 80).uniqueIndex()
    val password = varchar("password_hash", 100)
    override val primaryKey = PrimaryKey(id)
}

object Prizes : Table("prizes") {
    val id = varchar("id", 40)
    val year = integer("award_year")
    val category = varchar("category", 10)
    val name = text("full_name")
    override val primaryKey = PrimaryKey(id)
}

object Laureates : Table("laureates") {
    val id = varchar("id", 40)
    val prize = reference("prize_id", Prizes.id)
    val payload = text("payload")
    override val primaryKey = PrimaryKey(id, prize)
}

object Favorites : Table("user_prizes") {
    val user = reference("user_id", Users.id)
    val prize = reference("prize_id", Prizes.id)
    override val primaryKey = PrimaryKey(user, prize)
}

class DatabaseRepository : PrizeRepository {
    private val pool = HikariDataSource(HikariConfig().apply {
        jdbcUrl = System.getenv("JDBC_URL") ?: error("Нужен JDBC_URL Neon с sslmode=require")
        require(jdbcUrl.contains("sslmode=require")) { "Для Neon требуется SSL" }
        username = System.getenv("DB_USER") ?: error("Нужен DB_USER")
        password = System.getenv("DB_PASSWORD") ?: error("Нужен DB_PASSWORD")
        maximumPoolSize = 4
    })
    private val database = Database.connect(pool)

    init {
        transaction(database) {
            SchemaUtils.create(Users, Prizes, Laureates, Favorites)
            insertDemoUsers()
            insertSamplePrizes()
        }
    }

    private fun insertDemoUsers() {
        val demoUsers = listOf(
            Triple(1, "learner", System.getenv("DEMO_PASSWORD") ?: "practice"),
            Triple(2, "second", System.getenv("SECOND_DEMO_PASSWORD") ?: "practice2"),
        )
        for ((id, name, password) in demoUsers) {
            Users.insertIgnore {
                it[Users.id] = id
                it[username] = name
                it[Users.password] = BCrypt.hashpw(password, BCrypt.gensalt())
            }
        }
    }

    private fun insertSamplePrizes() {
        for (sample in seed()) {
            Prizes.insertIgnore {
                it[id] = sample.id
                it[year] = sample.year
                it[category] = sample.category
                it[name] = sample.fullName
            }
            for (laureate in sample.laureates) {
                Laureates.insertIgnore {
                    it[id] = laureate.id
                    it[prize] = sample.id
                    it[payload] = Json.encodeToString(Laureate.serializer(), laureate)
                }
            }
        }
    }

    private suspend fun <T> db(action: Transaction.() -> T): T = withContext(Dispatchers.IO) {
        transaction(database) { action() }
    }

    override suspend fun all() = db {
        Prizes.selectAll().map { row ->
            val id = row[Prizes.id]
            val laureates = Laureates.selectAll().where { Laureates.prize eq id }.map {
                Json.decodeFromString<Laureate>(it[Laureates.payload])
            }
            NobelPrize(id, row[Prizes.year], row[Prizes.category], row[Prizes.name], laureates)
        }
    }

    override suspend fun login(username: String, password: String) = db {
        val row = Users.selectAll().where { Users.username eq username }.singleOrNull()
        if (row != null && BCrypt.checkpw(password, row[Users.password])) {
            User(row[Users.id], row[Users.username])
        } else {
            null
        }
    }

    override suspend fun user(id: Int) = db {
        Users.selectAll().where { Users.id eq id }.singleOrNull()?.let {
            User(it[Users.id], it[Users.username])
        }
    }

    override suspend fun favorites(user: Int): List<NobelPrize> {
        val ids = db {
            Favorites.selectAll().where { Favorites.user eq user }.map { it[Favorites.prize] }.toSet()
        }
        return all().filter { it.id in ids }
    }

    override suspend fun add(user: Int, prize: String) = db {
        if (Prizes.selectAll().where { Prizes.id eq prize }.empty()) {
            false
        } else {
            Favorites.insertIgnore {
                it[Favorites.user] = user
                it[Favorites.prize] = prize
            }
            true
        }
    }

    override suspend fun remove(user: Int, prize: String) {
        db { Favorites.deleteWhere { (Favorites.user eq user) and (Favorites.prize eq prize) } }
    }

    fun close() {
        pool.close()
    }
}

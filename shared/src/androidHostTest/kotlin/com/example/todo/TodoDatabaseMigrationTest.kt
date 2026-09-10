package com.example.todo

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.todo.db.TodoDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * v1（期限・優先度・タグが入る前）の DB を作り、実際にマイグレーションを流して検証する。
 *
 * 既存のインストールを壊すと入っていた Todo が失われるため、
 * 「新しいスキーマで動くか」だけでなく「前のデータが残っているか」まで確認する。
 * JVM 上の SQLite で動かすので、このテストだけ androidHostTest に置いている。
 */
class TodoDatabaseMigrationTest {

    /** 変更前（v1）のスキーマ。当時の Todo.sq と同じ内容。 */
    private val schemaV1 = """
        CREATE TABLE todoEntity (
            id        INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            title     TEXT    NOT NULL,
            notes     TEXT    NOT NULL DEFAULT '',
            isDone    INTEGER NOT NULL DEFAULT 0,
            createdAt INTEGER NOT NULL
        );
        CREATE INDEX todoEntity_sort ON todoEntity(isDone, createdAt);
    """.trimIndent()

    @Test
    fun migrating_from_v1_keeps_existing_todos() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        schemaV1.split(";").filter { it.isNotBlank() }.forEach { statement ->
            driver.execute(null, statement, 0)
        }
        driver.execute(
            identifier = null,
            sql = "INSERT INTO todoEntity(title, notes, isDone, createdAt) VALUES ('牛乳を買う', 'メモ', 0, 100);",
            parameters = 0,
        )

        TodoDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = TodoDatabase.Schema.version)

        // 生成されたクエリで読めること = 新しいスキーマとして成立していること
        val todos = TodoDatabase(driver).todoQueries.selectAll().executeAsList()
        assertEquals(1, todos.size)

        val todo = todos.single()
        assertEquals("牛乳を買う", todo.title, "マイグレーション前の行が残っている")
        assertEquals("メモ", todo.notes)
        assertEquals(100, todo.createdAt)
        assertEquals(null, todo.dueAt, "既存の行は期限なしになる")
        assertEquals(0, todo.priority, "既存の行は優先度なし（0）になる")

        driver.close()
    }

    @Test
    fun migrated_database_accepts_tags() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        schemaV1.split(";").filter { it.isNotBlank() }.forEach { statement ->
            driver.execute(null, statement, 0)
        }

        TodoDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = TodoDatabase.Schema.version)

        // v1 には無かったタグ用のテーブルが使えること
        val queries = TodoDatabase(driver).todoQueries
        queries.insert(
            title = "掃除",
            notes = "",
            isDone = false,
            createdAt = 200,
            dueAt = 300,
            priority = 3,
        )
        val id = queries.selectAll().executeAsList().single().id
        queries.insertTag("家事")
        val tagId = queries.selectTagIdByName("家事").executeAsOne()
        queries.insertTagLink(todoId = id, tagId = tagId)

        val links = queries.selectAllTagLinks().executeAsList()
        assertEquals(1, links.size)
        assertEquals("家事", links.single().name)
        assertEquals(id, links.single().todoId)

        driver.close()
    }

    @Test
    fun fresh_install_matches_the_migrated_schema() {
        // .sq（最新スキーマ）と .sqm（v1 からの積み上げ）が食い違っていないかを見る。
        // 片方だけ直したときに気づけるようにするのがねらい。
        val fresh = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TodoDatabase.Schema.create(fresh)

        val migrated = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        schemaV1.split(";").filter { it.isNotBlank() }.forEach { statement ->
            migrated.execute(null, statement, 0)
        }
        TodoDatabase.Schema.migrate(migrated, oldVersion = 1, newVersion = TodoDatabase.Schema.version)

        assertEquals(tableColumns(fresh), tableColumns(migrated), "新規作成とマイグレーション後で列が揃っている")

        fresh.close()
        migrated.close()
    }

    /** テーブルごとの「列名:型:NOT NULL」を集めて、スキーマの形を比べられるようにする。 */
    private fun tableColumns(driver: JdbcSqliteDriver): Map<String, List<String>> {
        val tables = driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name;",
            mapper = { cursor ->
                val names = mutableListOf<String>()
                while (cursor.next().value) names += cursor.getString(0)!!
                QueryResult.Value(names.toList())
            },
            parameters = 0,
        ).value

        assertTrue(tables.contains("todoEntity"), "todoEntity が存在する")

        return tables.associateWith { table ->
            driver.executeQuery(
                identifier = null,
                sql = "PRAGMA table_info($table);",
                mapper = { cursor ->
                    val columns = mutableListOf<String>()
                    while (cursor.next().value) {
                        // 1 = name, 2 = type, 3 = notnull
                        columns += "${cursor.getString(1)}:${cursor.getString(2)}:${cursor.getLong(3)}"
                    }
                    QueryResult.Value(columns.sorted().toList())
                },
                parameters = 0,
            ).value
        }
    }
}

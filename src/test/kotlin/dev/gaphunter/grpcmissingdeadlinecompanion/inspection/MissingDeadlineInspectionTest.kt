package dev.gaphunter.grpcmissingdeadlinecompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MissingDeadlineInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(MissingDeadlineInspection::class.java)
    }

    fun `test a blocking stub RPC call with no deadline is flagged`() {
        myFixture.configureByText(
            "UserServiceClient.java",
            """
            class UserServiceClient {
                void fetch(io.grpc.Channel channel, GetUserRequest request) {
                    UserServiceGrpc.newBlockingStub(channel).getUser(request);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("no deadline") == true })
    }

    fun `test withDeadlineAfter before the RPC call is not flagged`() {
        myFixture.configureByText(
            "UserServiceClient2.java",
            """
            import java.util.concurrent.TimeUnit;

            class UserServiceClient2 {
                void fetch(io.grpc.Channel channel, GetUserRequest request) {
                    UserServiceGrpc.newBlockingStub(channel).withDeadlineAfter(5, TimeUnit.SECONDS).getUser(request);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("no deadline") == true })
    }

    fun `test withDeadline before the RPC call is not flagged`() {
        myFixture.configureByText(
            "UserServiceClient3.java",
            """
            class UserServiceClient3 {
                void fetch(io.grpc.Channel channel, io.grpc.Deadline deadline, GetUserRequest request) {
                    UserServiceGrpc.newBlockingStub(channel).withDeadline(deadline).getUser(request);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("no deadline") == true })
    }

    fun `test a dead-end chain with no real RPC call is not flagged`() {
        myFixture.configureByText(
            "UserServiceClient4.java",
            """
            import java.util.concurrent.TimeUnit;

            class UserServiceClient4 {
                Object buildStub(io.grpc.Channel channel) {
                    return UserServiceGrpc.newBlockingStub(channel).withDeadlineAfter(5, TimeUnit.SECONDS);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("no deadline") == true })
    }

    fun `test an unrelated method call is never flagged`() {
        myFixture.configureByText(
            "PlainClient.java",
            """
            class PlainClient {
                void fetch(SomeOtherBuilder builder) {
                    builder.build().call();
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("no deadline") == true })
    }
}

package com.trendyol.kediatr.koin

import com.trendyol.kediatr.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.test.junit5.KoinTestExtension
import kotlin.test.assertTrue

var asyncPipelineProcessCounter = 0
var pipelineProcessCounter = 0

class PipelineBehaviorTest: KoinTest {

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(
            module {
                single { KediatrKoin.getCommandBus() }
                single { MyPipelineBehavior(get()) } bind PipelineBehavior::class
                single { MyAsyncPipelineBehavior(get()) } bind AsyncPipelineBehavior::class
                single { MyCommandHandler(get()) } bind CommandHandler::class
                single { MyAsyncCommandHandler(get()) } bind AsyncCommandHandler::class
                single { MyCommandRHandler(get()) } bind CommandWithResultHandler::class
                single { MyAsyncCommandRHandler(get()) } bind AsyncCommandWithResultHandler::class
                single { MyFirstNotificationHandler(get()) } bind NotificationHandler::class
                single { MyFirstAsyncNotificationHandler(get()) } bind AsyncNotificationHandler::class
                single { MySecondNotificationHandler(get()) } bind NotificationHandler::class
                single { TestQueryHandler(get()) } bind QueryHandler::class
                single { AsyncTestQueryHandler(get()) } bind AsyncQueryHandler::class
            },
        )
    }

    init {
        asyncPipelineProcessCounter = 0
        pipelineProcessCounter = 0
    }

    private val commandBus by inject<CommandBus>()

    @Test
    fun `should process command with pipeline`() {
        commandBus.executeCommand(MyCommand())

        assertTrue { pipelineProcessCounter == 1 }
    }

    @Test
    fun `should process command with async pipeline`() {
        runBlocking {
            commandBus.executeCommandAsync(MyCommand())

        }

        assertTrue { asyncPipelineProcessCounter == 1 }
    }

}

class MyBrokenCommand(override val commandMetadata: CommandMetadata? = null) : Command

class MyBrokenHandler(
    private val commandBus: CommandBus
) : CommandHandler<MyBrokenCommand> {
    override fun handle(command: MyBrokenCommand) {
        throw Exception()
    }
}

class MyBrokenAsyncHandler(
    private val commandBus: CommandBus
) : AsyncCommandHandler<MyBrokenCommand> {
    override suspend fun handleAsync(command: MyBrokenCommand) {
        delay(500)
        throw Exception()
    }
}

class MyPipelineBehavior(
    private val commandBus: CommandBus
) : PipelineBehavior{
    override fun  <TRequest, TResponse> process(request: TRequest, act: () -> TResponse): TResponse {
        pipelineProcessCounter++
        return act()
    }
}

class MyAsyncPipelineBehavior(
    private val commandBus: CommandBus
) : AsyncPipelineBehavior {
    override suspend fun  <TRequest, TResponse> process(request: TRequest, act: suspend () -> TResponse): TResponse {
        asyncPipelineProcessCounter++
        return act()
    }
}
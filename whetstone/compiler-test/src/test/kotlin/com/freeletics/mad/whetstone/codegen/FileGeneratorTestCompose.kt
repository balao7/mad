package com.freeletics.mad.whetstone.codegen

import com.freeletics.mad.whetstone.AppScope
import com.freeletics.mad.whetstone.ComposableParameter
import com.freeletics.mad.whetstone.ComposeScreenData
import com.freeletics.mad.whetstone.NavEntryData
import com.freeletics.mad.whetstone.Navigation
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import org.junit.Test

internal class FileGeneratorTestCompose {

    private val navigation = Navigation.Compose(
        route = ClassName("com.test", "TestRoute"),
        destinationType = "SCREEN",
        destinationScope = ClassName("com.test.destination", "TestDestinationScope"),
    )

    private val data = ComposeScreenData(
        baseName = "Test",
        packageName = "com.test",
        scope = ClassName("com.test", "TestScreen"),
        parentScope = ClassName("com.test.parent", "TestParentScope"),
        stateMachine = ClassName("com.test", "TestStateMachine"),
        navigation = null,
        navEntryData = null,
        composableParameter = emptyList(),
        stateParameter = ComposableParameter("state", ClassName("com.test", "TestState")),
        sendActionParameter = ComposableParameter(
            "sendAction",
            Function1::class.asClassName().parameterizedBy(
                ClassName("com.test", "TestAction"),
                UNIT,
            ),
        ),
    )

    private val navEntryData = NavEntryData(
        packageName = "com.test",
        scope = ClassName("com.test", "TestScreen"),
        parentScope = ClassName("com.test.parent", "TestParentScope"),
        navigation = navigation,
    )

    @Test
    fun `generates code for ComposeScreenData`() {
        val source = """
            package com.test
            
            import androidx.compose.runtime.Composable
            import com.freeletics.mad.whetstone.compose.ComposeScreen
            import com.test.parent.TestParentScope
            
            @ComposeScreen(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
              stateMachine = TestStateMachine::class,
            )
            @Composable
            @Suppress("unused_parameter")
            public fun Test(
              state: TestState,
              sendAction: (TestAction) -> Unit
            ) {}
        """.trimIndent()

        val expected = """
            package com.test

            import android.os.Bundle
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.runtime.rememberCoroutineScope
            import androidx.lifecycle.SavedStateHandle
            import com.freeletics.mad.whetstone.ScopeTo
            import com.freeletics.mad.whetstone.`internal`.InternalWhetstoneApi
            import com.freeletics.mad.whetstone.`internal`.asComposeState
            import com.freeletics.mad.whetstone.compose.`internal`.rememberComponent
            import com.squareup.anvil.annotations.ContributesSubcomponent
            import com.squareup.anvil.annotations.ContributesTo
            import com.test.parent.TestParentScope
            import dagger.BindsInstance
            import dagger.Module
            import dagger.multibindings.Multibinds
            import java.io.Closeable
            import kotlin.OptIn
            import kotlin.collections.Set
            import kotlinx.coroutines.launch

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestScreen::class)
            @ContributesSubcomponent(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
            )
            public interface WhetstoneTestComponent : Closeable {
              public val testStateMachine: TestStateMachine

              public val closeables: Set<Closeable>
    
              override fun close() {
                closeables.forEach {
                  it.close()
                }
              }

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance savedStateHandle: SavedStateHandle, @BindsInstance
                    arguments: Bundle): WhetstoneTestComponent
              }

              @ContributesTo(TestParentScope::class)
              public interface ParentComponent {
                public fun whetstoneTestComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestScreen::class)
            public interface WhetstoneTestModule {
              @Multibinds
              public fun bindCloseables(): Set<Closeable>
            }

            @Composable
            @OptIn(InternalWhetstoneApi::class)
            public fun WhetstoneTest(arguments: Bundle) {
              val component = rememberComponent(TestParentScope::class, arguments) { parentComponent:
                  WhetstoneTestComponent.ParentComponent, savedStateHandle, argumentsForComponent ->
                parentComponent.whetstoneTestComponentFactory().create(savedStateHandle, argumentsForComponent)
              }

              WhetstoneTest(component)
            }
            
            @Composable
            @OptIn(InternalWhetstoneApi::class)
            private fun WhetstoneTest(component: WhetstoneTestComponent) {
              val stateMachine = remember { component.testStateMachine }
              val state = stateMachine.asComposeState()
              val currentState = state.value
              if (currentState != null) {
                val scope = rememberCoroutineScope()
                Test(
                  state = currentState,
                  sendAction = { scope.launch { stateMachine.dispatch(it) } },
                )
              }
            }
            
        """.trimIndent()

        test(data, "com/test/Test.kt", source, expected)
    }

    @Test
    fun `generates code for ComposeScreenData with navigation`() {
        val withNavigation = data.copy(
            scope = navigation.route,
            navigation = navigation,
        )

        val source = """
            package com.test
            
            import androidx.compose.runtime.Composable
            import com.freeletics.mad.whetstone.compose.ComposeDestination
            import com.freeletics.mad.whetstone.compose.DestinationType
            import com.test.destination.TestDestinationScope
            import com.test.parent.TestParentScope
            
            @ComposeDestination(
              route = TestRoute::class,
              parentScope = TestParentScope::class,
              stateMachine = TestStateMachine::class,
              destinationType = DestinationType.SCREEN,
              destinationScope = TestDestinationScope::class,
            )
            @Composable
            @Suppress("unused_parameter")
            public fun Test(
              state: TestState,
              sendAction: (TestAction) -> Unit
            ) {}
        """.trimIndent()

        val expected = """
            package com.test

            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.runtime.rememberCoroutineScope
            import androidx.lifecycle.SavedStateHandle
            import com.freeletics.mad.navigator.NavEventNavigator
            import com.freeletics.mad.navigator.compose.NavDestination
            import com.freeletics.mad.navigator.compose.NavigationSetup
            import com.freeletics.mad.navigator.compose.ScreenDestination
            import com.freeletics.mad.whetstone.ScopeTo
            import com.freeletics.mad.whetstone.`internal`.InternalWhetstoneApi
            import com.freeletics.mad.whetstone.`internal`.asComposeState
            import com.freeletics.mad.whetstone.compose.`internal`.rememberComponent
            import com.squareup.anvil.annotations.ContributesSubcomponent
            import com.squareup.anvil.annotations.ContributesTo
            import com.test.destination.TestDestinationScope
            import com.test.parent.TestParentScope
            import dagger.BindsInstance
            import dagger.Module
            import dagger.Provides
            import dagger.multibindings.IntoSet
            import dagger.multibindings.Multibinds
            import java.io.Closeable
            import kotlin.OptIn
            import kotlin.collections.Set
            import kotlinx.coroutines.launch

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestRoute::class)
            @ContributesSubcomponent(
              scope = TestRoute::class,
              parentScope = TestParentScope::class,
            )
            public interface WhetstoneTestComponent : Closeable {
              public val testStateMachine: TestStateMachine

              public val navEventNavigator: NavEventNavigator

              public val closeables: Set<Closeable>
    
              override fun close() {
                closeables.forEach {
                  it.close()
                }
              }

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance savedStateHandle: SavedStateHandle, @BindsInstance
                    testRoute: TestRoute): WhetstoneTestComponent
              }

              @ContributesTo(TestParentScope::class)
              public interface ParentComponent {
                public fun whetstoneTestComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestRoute::class)
            public interface WhetstoneTestModule {
              @Multibinds
              public fun bindCloseables(): Set<Closeable>
            }

            @Composable
            @OptIn(InternalWhetstoneApi::class)
            public fun WhetstoneTest(testRoute: TestRoute) {
              val component = rememberComponent(TestParentScope::class, TestDestinationScope::class, testRoute)
                  { parentComponent: WhetstoneTestComponent.ParentComponent, savedStateHandle,
                  testRouteForComponent ->
                parentComponent.whetstoneTestComponentFactory().create(savedStateHandle, testRouteForComponent)
              }

              NavigationSetup(component.navEventNavigator)

              WhetstoneTest(component)
            }
            
            @Composable
            @OptIn(InternalWhetstoneApi::class)
            private fun WhetstoneTest(component: WhetstoneTestComponent) {
              val stateMachine = remember { component.testStateMachine }
              val state = stateMachine.asComposeState()
              val currentState = state.value
              if (currentState != null) {
                val scope = rememberCoroutineScope()
                Test(
                  state = currentState,
                  sendAction = { scope.launch { stateMachine.dispatch(it) } },
                )
              }
            }
            
            @Module
            @ContributesTo(TestDestinationScope::class)
            public object WhetstoneTestNavDestinationModule {
              @Provides
              @IntoSet
              public fun provideNavDestination(): NavDestination = ScreenDestination<TestRoute> {
                WhetstoneTest(it)
              }
            }
            
        """.trimIndent()

        test(withNavigation, "com/test/Test.kt", source, expected)
    }

    @Test
    fun `generates code for ComposeScreenData with navigation, destination and navEntry`() {
        val withNavEntry = data.copy(
            scope = navigation.route,
            navigation = navigation,
            navEntryData = navEntryData,
        )

        val source = """
            package com.test
            
            import androidx.compose.runtime.Composable
            import com.freeletics.mad.whetstone.compose.ComposeDestination
            import com.freeletics.mad.whetstone.compose.DestinationType
            import com.freeletics.mad.whetstone.NavEntryComponent
            import com.test.destination.TestDestinationScope
            import com.test.parent.TestParentScope
            
            @ComposeDestination(
              route = TestRoute::class,
              parentScope = TestParentScope::class,
              stateMachine = TestStateMachine::class,
              destinationType = DestinationType.SCREEN,
              destinationScope = TestDestinationScope::class,
            )
            @NavEntryComponent(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
            )
            @Composable
            @Suppress("unused_parameter")
            public fun Test(
              state: TestState,
              sendAction: (TestAction) -> Unit
            ) {}
        """.trimIndent()

        val expected = """
            package com.test

            import android.content.Context
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.runtime.rememberCoroutineScope
            import androidx.lifecycle.SavedStateHandle
            import com.freeletics.mad.navigator.NavEventNavigator
            import com.freeletics.mad.navigator.`internal`.InternalNavigatorApi
            import com.freeletics.mad.navigator.`internal`.NavigationExecutor
            import com.freeletics.mad.navigator.compose.NavDestination
            import com.freeletics.mad.navigator.compose.NavigationSetup
            import com.freeletics.mad.navigator.compose.ScreenDestination
            import com.freeletics.mad.whetstone.NavEntry
            import com.freeletics.mad.whetstone.ScopeTo
            import com.freeletics.mad.whetstone.`internal`.DestinationComponent
            import com.freeletics.mad.whetstone.`internal`.InternalWhetstoneApi
            import com.freeletics.mad.whetstone.`internal`.NavEntryComponentGetter
            import com.freeletics.mad.whetstone.`internal`.NavEntryComponentGetterKey
            import com.freeletics.mad.whetstone.`internal`.asComposeState
            import com.freeletics.mad.whetstone.`internal`.navEntryComponent
            import com.freeletics.mad.whetstone.compose.`internal`.rememberComponent
            import com.squareup.anvil.annotations.ContributesMultibinding
            import com.squareup.anvil.annotations.ContributesSubcomponent
            import com.squareup.anvil.annotations.ContributesTo
            import com.test.destination.TestDestinationScope
            import com.test.parent.TestParentScope
            import dagger.BindsInstance
            import dagger.Module
            import dagger.Provides
            import dagger.multibindings.IntoSet
            import dagger.multibindings.Multibinds
            import java.io.Closeable
            import javax.inject.Inject
            import kotlin.Any
            import kotlin.OptIn
            import kotlin.collections.Set
            import kotlinx.coroutines.launch

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestRoute::class)
            @ContributesSubcomponent(
              scope = TestRoute::class,
              parentScope = TestParentScope::class,
            )
            public interface WhetstoneTestComponent : Closeable {
              public val testStateMachine: TestStateMachine

              public val navEventNavigator: NavEventNavigator

              public val closeables: Set<Closeable>
    
              override fun close() {
                closeables.forEach {
                  it.close()
                }
              }

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance savedStateHandle: SavedStateHandle, @BindsInstance
                    testRoute: TestRoute): WhetstoneTestComponent
              }

              @ContributesTo(TestParentScope::class)
              public interface ParentComponent {
                public fun whetstoneTestComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestRoute::class)
            public interface WhetstoneTestModule {
              @Multibinds
              public fun bindCloseables(): Set<Closeable>
            }

            @Composable
            @OptIn(InternalWhetstoneApi::class)
            public fun WhetstoneTest(testRoute: TestRoute) {
              val component = rememberComponent(TestParentScope::class, TestDestinationScope::class, testRoute)
                  { parentComponent: WhetstoneTestComponent.ParentComponent, savedStateHandle,
                  testRouteForComponent ->
                parentComponent.whetstoneTestComponentFactory().create(savedStateHandle, testRouteForComponent)
              }

              NavigationSetup(component.navEventNavigator)

              WhetstoneTest(component)
            }

            @Composable
            @OptIn(InternalWhetstoneApi::class)
            private fun WhetstoneTest(component: WhetstoneTestComponent) {
              val stateMachine = remember { component.testStateMachine }
              val state = stateMachine.asComposeState()
              val currentState = state.value
              if (currentState != null) {
                val scope = rememberCoroutineScope()
                Test(
                  state = currentState,
                  sendAction = { scope.launch { stateMachine.dispatch(it) } },
                )
              }
            }

            @Module
            @ContributesTo(TestDestinationScope::class)
            public object WhetstoneTestNavDestinationModule {
              @Provides
              @IntoSet
              public fun provideNavDestination(): NavDestination = ScreenDestination<TestRoute> {
                WhetstoneTest(it)
              }
            }

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestScreen::class)
            @ContributesSubcomponent(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
            )
            public interface WhetstoneTestScreenNavEntryComponent : Closeable {
              @get:NavEntry(TestScreen::class)
              public val closeables: Set<Closeable>
    
              override fun close() {
                closeables.forEach {
                  it.close()
                }
              }

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance @NavEntry(TestScreen::class)
                    savedStateHandle: SavedStateHandle, @BindsInstance @NavEntry(TestScreen::class)
                    testRoute: TestRoute): WhetstoneTestScreenNavEntryComponent
              }

              @ContributesTo(TestParentScope::class)
              public interface ParentComponent {
                public fun whetstoneTestScreenNavEntryComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestScreen::class)
            public interface WhetstoneTestScreenNavEntryModule {
              @Multibinds
              @NavEntry(TestScreen::class)
              public fun bindCloseables(): Set<Closeable>
            }

            @OptIn(InternalWhetstoneApi::class)
            @NavEntryComponentGetterKey(TestScreen::class)
            @ContributesMultibinding(
              TestDestinationScope::class,
              NavEntryComponentGetter::class,
            )
            public class TestScreenNavEntryComponentGetter @Inject constructor() : NavEntryComponentGetter {
              @OptIn(InternalWhetstoneApi::class, InternalNavigatorApi::class)
              override fun retrieve(executor: NavigationExecutor, context: Context): Any =
                  navEntryComponent(TestRoute::class, executor, context, TestParentScope::class,
                  TestDestinationScope::class) { parentComponent:
                  WhetstoneTestScreenNavEntryComponent.ParentComponent, savedStateHandle, testRoute ->
                parentComponent.whetstoneTestScreenNavEntryComponentFactory().create(savedStateHandle,
                    testRoute)
              }
            }

            @ContributesTo(TestDestinationScope::class)
            @OptIn(InternalWhetstoneApi::class)
            public interface WhetstoneTestScreenNavEntryDestinationComponent : DestinationComponent

        """.trimIndent()

        test(withNavEntry, "com/test/Test.kt", source, expected)
    }

    @Test
    fun `generates code for ComposeScreenData with default values`() {
        val navigation = navigation.copy(destinationScope = AppScope::class.asClassName())
        val withDefaultValues = data.copy(
            scope = navigation.route,
            parentScope = AppScope::class.asClassName(),
            navigation = navigation,
            navEntryData = navEntryData.copy(
                parentScope = AppScope::class.asClassName(),
                navigation = navigation,
            ),
        )

        val source = """
            package com.test
            
            import androidx.compose.runtime.Composable
            import com.freeletics.mad.whetstone.compose.ComposeDestination
            import com.freeletics.mad.whetstone.compose.DestinationType
            import com.freeletics.mad.whetstone.NavEntryComponent
            
            @ComposeDestination(
              route = TestRoute::class,
              stateMachine = TestStateMachine::class,
              destinationType = DestinationType.SCREEN,
            )
            @NavEntryComponent(
              scope = TestScreen::class,
            )
            @Composable
            @Suppress("unused_parameter")
            public fun Test(
              state: TestState,
              sendAction: (TestAction) -> Unit
            ) {}
        """.trimIndent()

        val expected = """
            package com.test

            import android.content.Context
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.runtime.rememberCoroutineScope
            import androidx.lifecycle.SavedStateHandle
            import com.freeletics.mad.navigator.NavEventNavigator
            import com.freeletics.mad.navigator.`internal`.InternalNavigatorApi
            import com.freeletics.mad.navigator.`internal`.NavigationExecutor
            import com.freeletics.mad.navigator.compose.NavDestination
            import com.freeletics.mad.navigator.compose.NavigationSetup
            import com.freeletics.mad.navigator.compose.ScreenDestination
            import com.freeletics.mad.whetstone.AppScope
            import com.freeletics.mad.whetstone.NavEntry
            import com.freeletics.mad.whetstone.ScopeTo
            import com.freeletics.mad.whetstone.`internal`.DestinationComponent
            import com.freeletics.mad.whetstone.`internal`.InternalWhetstoneApi
            import com.freeletics.mad.whetstone.`internal`.NavEntryComponentGetter
            import com.freeletics.mad.whetstone.`internal`.NavEntryComponentGetterKey
            import com.freeletics.mad.whetstone.`internal`.asComposeState
            import com.freeletics.mad.whetstone.`internal`.navEntryComponent
            import com.freeletics.mad.whetstone.compose.`internal`.rememberComponent
            import com.squareup.anvil.annotations.ContributesMultibinding
            import com.squareup.anvil.annotations.ContributesSubcomponent
            import com.squareup.anvil.annotations.ContributesTo
            import dagger.BindsInstance
            import dagger.Module
            import dagger.Provides
            import dagger.multibindings.IntoSet
            import dagger.multibindings.Multibinds
            import java.io.Closeable
            import javax.inject.Inject
            import kotlin.Any
            import kotlin.OptIn
            import kotlin.collections.Set
            import kotlinx.coroutines.launch

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestRoute::class)
            @ContributesSubcomponent(
              scope = TestRoute::class,
              parentScope = AppScope::class,
            )
            public interface WhetstoneTestComponent : Closeable {
              public val testStateMachine: TestStateMachine

              public val navEventNavigator: NavEventNavigator

              public val closeables: Set<Closeable>
    
              override fun close() {
                closeables.forEach {
                  it.close()
                }
              }

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance savedStateHandle: SavedStateHandle, @BindsInstance
                    testRoute: TestRoute): WhetstoneTestComponent
              }

              @ContributesTo(AppScope::class)
              public interface ParentComponent {
                public fun whetstoneTestComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestRoute::class)
            public interface WhetstoneTestModule {
              @Multibinds
              public fun bindCloseables(): Set<Closeable>
            }

            @Composable
            @OptIn(InternalWhetstoneApi::class)
            public fun WhetstoneTest(testRoute: TestRoute) {
              val component = rememberComponent(AppScope::class, AppScope::class, testRoute) { parentComponent:
                  WhetstoneTestComponent.ParentComponent, savedStateHandle, testRouteForComponent ->
                parentComponent.whetstoneTestComponentFactory().create(savedStateHandle, testRouteForComponent)
              }

              NavigationSetup(component.navEventNavigator)

              WhetstoneTest(component)
            }

            @Composable
            @OptIn(InternalWhetstoneApi::class)
            private fun WhetstoneTest(component: WhetstoneTestComponent) {
              val stateMachine = remember { component.testStateMachine }
              val state = stateMachine.asComposeState()
              val currentState = state.value
              if (currentState != null) {
                val scope = rememberCoroutineScope()
                Test(
                  state = currentState,
                  sendAction = { scope.launch { stateMachine.dispatch(it) } },
                )
              }
            }

            @Module
            @ContributesTo(AppScope::class)
            public object WhetstoneTestNavDestinationModule {
              @Provides
              @IntoSet
              public fun provideNavDestination(): NavDestination = ScreenDestination<TestRoute> {
                WhetstoneTest(it)
              }
            }

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestScreen::class)
            @ContributesSubcomponent(
              scope = TestScreen::class,
              parentScope = AppScope::class,
            )
            public interface WhetstoneTestScreenNavEntryComponent : Closeable {
              @get:NavEntry(TestScreen::class)
              public val closeables: Set<Closeable>
    
              override fun close() {
                closeables.forEach {
                  it.close()
                }
              }

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance @NavEntry(TestScreen::class)
                    savedStateHandle: SavedStateHandle, @BindsInstance @NavEntry(TestScreen::class)
                    testRoute: TestRoute): WhetstoneTestScreenNavEntryComponent
              }

              @ContributesTo(AppScope::class)
              public interface ParentComponent {
                public fun whetstoneTestScreenNavEntryComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestScreen::class)
            public interface WhetstoneTestScreenNavEntryModule {
              @Multibinds
              @NavEntry(TestScreen::class)
              public fun bindCloseables(): Set<Closeable>
            }

            @OptIn(InternalWhetstoneApi::class)
            @NavEntryComponentGetterKey(TestScreen::class)
            @ContributesMultibinding(
              AppScope::class,
              NavEntryComponentGetter::class,
            )
            public class TestScreenNavEntryComponentGetter @Inject constructor() : NavEntryComponentGetter {
              @OptIn(InternalWhetstoneApi::class, InternalNavigatorApi::class)
              override fun retrieve(executor: NavigationExecutor, context: Context): Any =
                  navEntryComponent(TestRoute::class, executor, context, AppScope::class, AppScope::class) {
                  parentComponent: WhetstoneTestScreenNavEntryComponent.ParentComponent, savedStateHandle,
                  testRoute ->
                parentComponent.whetstoneTestScreenNavEntryComponentFactory().create(savedStateHandle,
                    testRoute)
              }
            }

            @ContributesTo(AppScope::class)
            @OptIn(InternalWhetstoneApi::class)
            public interface WhetstoneTestScreenNavEntryDestinationComponent : DestinationComponent

        """.trimIndent()

        test(withDefaultValues, "com/test/Test.kt", source, expected)
    }

    @Test
    fun `generates code for ComposeScreenData with Composable Dependencies`() {
        val withInjectedParameters = data.copy(
            baseName = "Test2",
            composableParameter = listOf(
                ComposableParameter(
                    name = "testClass",
                    typeName = ClassName("com.test", "TestClass"),
                ),
                ComposableParameter(
                    name = "test",
                    typeName = ClassName("com.test.other", "TestClass2"),
                ),
                ComposableParameter(
                    name = "testSet",
                    typeName = SET.parameterizedBy(STRING),
                ),
                ComposableParameter(
                    name = "testMap",
                    typeName = MAP.parameterizedBy(STRING, INT),
                ),
            ),
        )

        val source = """
            package com.test
            
            import androidx.compose.runtime.Composable
            import com.freeletics.mad.whetstone.compose.ComposeScreen
            import com.test.other.TestClass2
            import com.test.parent.TestParentScope
            
            @ComposeScreen(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
              stateMachine = TestStateMachine::class,
            )
            @Composable
            @Suppress("unused_parameter")
            public fun Test2(
                state: TestState,
                sendAction: (TestAction) -> Unit,
                testClass: TestClass,
                test: TestClass2,
                testSet: Set<String>,
                testMap: Map<String, Int>,
            ) {}
        """.trimIndent()

        val expected = """
            package com.test

            import android.os.Bundle
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.runtime.rememberCoroutineScope
            import androidx.lifecycle.SavedStateHandle
            import com.freeletics.mad.whetstone.ScopeTo
            import com.freeletics.mad.whetstone.`internal`.InternalWhetstoneApi
            import com.freeletics.mad.whetstone.`internal`.asComposeState
            import com.freeletics.mad.whetstone.compose.`internal`.rememberComponent
            import com.squareup.anvil.annotations.ContributesSubcomponent
            import com.squareup.anvil.annotations.ContributesTo
            import com.test.other.TestClass2
            import com.test.parent.TestParentScope
            import dagger.BindsInstance
            import dagger.Module
            import dagger.multibindings.Multibinds
            import java.io.Closeable
            import kotlin.Int
            import kotlin.OptIn
            import kotlin.String
            import kotlin.collections.Map
            import kotlin.collections.Set
            import kotlinx.coroutines.launch

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestScreen::class)
            @ContributesSubcomponent(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
            )
            public interface WhetstoneTest2Component : Closeable {
              public val testStateMachine: TestStateMachine

              public val testClass: TestClass

              public val test: TestClass2
            
              public val testSet: Set<String>

              public val testMap: Map<String, Int>

              public val closeables: Set<Closeable>
    
              override fun close() {
                closeables.forEach {
                  it.close()
                }
              }

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance savedStateHandle: SavedStateHandle, @BindsInstance
                    arguments: Bundle): WhetstoneTest2Component
              }

              @ContributesTo(TestParentScope::class)
              public interface ParentComponent {
                public fun whetstoneTest2ComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestScreen::class)
            public interface WhetstoneTest2Module {
              @Multibinds
              public fun bindCloseables(): Set<Closeable>
            }

            @Composable
            @OptIn(InternalWhetstoneApi::class)
            public fun WhetstoneTest2(arguments: Bundle) {
              val component = rememberComponent(TestParentScope::class, arguments) { parentComponent:
                  WhetstoneTest2Component.ParentComponent, savedStateHandle, argumentsForComponent ->
                parentComponent.whetstoneTest2ComponentFactory().create(savedStateHandle, argumentsForComponent)
              }

              WhetstoneTest2(component)
            }
            
            @Composable
            @OptIn(InternalWhetstoneApi::class)
            private fun WhetstoneTest2(component: WhetstoneTest2Component) {
              val testClass = remember { component.testClass }
              val test = remember { component.test }
              val testSet = remember { component.testSet }
              val testMap = remember { component.testMap }
              val stateMachine = remember { component.testStateMachine }
              val state = stateMachine.asComposeState()
              val currentState = state.value
              if (currentState != null) {
                val scope = rememberCoroutineScope()
                Test2(
                  testClass = testClass,
                  test = test,
                  testSet = testSet,
                  testMap = testMap,
                  state = currentState,
                  sendAction = { scope.launch { stateMachine.dispatch(it) } },
                )
              }
            }
            
        """.trimIndent()

        test(withInjectedParameters, "com/test/Test2.kt", source, expected)
    }

    @Test
    fun `generates code for ComposeScreenData without sendAction`() {
        val withoutSendAction = data.copy(
            sendActionParameter = null,
        )

        val source = """
            package com.test
            
            import androidx.compose.runtime.Composable
            import com.freeletics.mad.whetstone.compose.ComposeScreen
            import com.test.parent.TestParentScope
            
            @ComposeScreen(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
              stateMachine = TestStateMachine::class,
            )
            @Composable
            @Suppress("unused_parameter")
            public fun Test(
              state: TestState,
            ) {}
        """.trimIndent()

        val expected = """
            package com.test

            import android.os.Bundle
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.lifecycle.SavedStateHandle
            import com.freeletics.mad.whetstone.ScopeTo
            import com.freeletics.mad.whetstone.`internal`.InternalWhetstoneApi
            import com.freeletics.mad.whetstone.`internal`.asComposeState
            import com.freeletics.mad.whetstone.compose.`internal`.rememberComponent
            import com.squareup.anvil.annotations.ContributesSubcomponent
            import com.squareup.anvil.annotations.ContributesTo
            import com.test.parent.TestParentScope
            import dagger.BindsInstance
            import dagger.Module
            import dagger.multibindings.Multibinds
            import java.io.Closeable
            import kotlin.OptIn
            import kotlin.collections.Set

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestScreen::class)
            @ContributesSubcomponent(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
            )
            public interface WhetstoneTestComponent : Closeable {
              public val testStateMachine: TestStateMachine

              public val closeables: Set<Closeable>
    
              override fun close() {
                closeables.forEach {
                  it.close()
                }
              }

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance savedStateHandle: SavedStateHandle, @BindsInstance
                    arguments: Bundle): WhetstoneTestComponent
              }

              @ContributesTo(TestParentScope::class)
              public interface ParentComponent {
                public fun whetstoneTestComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestScreen::class)
            public interface WhetstoneTestModule {
              @Multibinds
              public fun bindCloseables(): Set<Closeable>
            }

            @Composable
            @OptIn(InternalWhetstoneApi::class)
            public fun WhetstoneTest(arguments: Bundle) {
              val component = rememberComponent(TestParentScope::class, arguments) { parentComponent:
                  WhetstoneTestComponent.ParentComponent, savedStateHandle, argumentsForComponent ->
                parentComponent.whetstoneTestComponentFactory().create(savedStateHandle, argumentsForComponent)
              }

              WhetstoneTest(component)
            }
            
            @Composable
            @OptIn(InternalWhetstoneApi::class)
            private fun WhetstoneTest(component: WhetstoneTestComponent) {
              val stateMachine = remember { component.testStateMachine }
              val state = stateMachine.asComposeState()
              val currentState = state.value
              if (currentState != null) {
                Test(
                  state = currentState,
                )
              }
            }
            
        """.trimIndent()

        test(withoutSendAction, "com/test/Test.kt", source, expected)
    }

    @Test
    fun `generates code for ComposeScreenData without state`() {
        val withoutSendAction = data.copy(
            stateParameter = null,
        )

        val source = """
            package com.test
            
            import androidx.compose.runtime.Composable
            import com.freeletics.mad.whetstone.compose.ComposeScreen
            import com.test.parent.TestParentScope
            
            @ComposeScreen(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
              stateMachine = TestStateMachine::class,
            )
            @Composable
            @Suppress("unused_parameter")
            public fun Test(
              sendAction: (TestAction) -> Unit
            ) {}
        """.trimIndent()

        val expected = """
            package com.test

            import android.os.Bundle
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.runtime.rememberCoroutineScope
            import androidx.lifecycle.SavedStateHandle
            import com.freeletics.mad.whetstone.ScopeTo
            import com.freeletics.mad.whetstone.`internal`.InternalWhetstoneApi
            import com.freeletics.mad.whetstone.`internal`.asComposeState
            import com.freeletics.mad.whetstone.compose.`internal`.rememberComponent
            import com.squareup.anvil.annotations.ContributesSubcomponent
            import com.squareup.anvil.annotations.ContributesTo
            import com.test.parent.TestParentScope
            import dagger.BindsInstance
            import dagger.Module
            import dagger.multibindings.Multibinds
            import java.io.Closeable
            import kotlin.OptIn
            import kotlin.collections.Set
            import kotlinx.coroutines.launch

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestScreen::class)
            @ContributesSubcomponent(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
            )
            public interface WhetstoneTestComponent : Closeable {
              public val testStateMachine: TestStateMachine

              public val closeables: Set<Closeable>
    
              override fun close() {
                closeables.forEach {
                  it.close()
                }
              }

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance savedStateHandle: SavedStateHandle, @BindsInstance
                    arguments: Bundle): WhetstoneTestComponent
              }

              @ContributesTo(TestParentScope::class)
              public interface ParentComponent {
                public fun whetstoneTestComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestScreen::class)
            public interface WhetstoneTestModule {
              @Multibinds
              public fun bindCloseables(): Set<Closeable>
            }

            @Composable
            @OptIn(InternalWhetstoneApi::class)
            public fun WhetstoneTest(arguments: Bundle) {
              val component = rememberComponent(TestParentScope::class, arguments) { parentComponent:
                  WhetstoneTestComponent.ParentComponent, savedStateHandle, argumentsForComponent ->
                parentComponent.whetstoneTestComponentFactory().create(savedStateHandle, argumentsForComponent)
              }

              WhetstoneTest(component)
            }
            
            @Composable
            @OptIn(InternalWhetstoneApi::class)
            private fun WhetstoneTest(component: WhetstoneTestComponent) {
              val stateMachine = remember { component.testStateMachine }
              val state = stateMachine.asComposeState()
              val currentState = state.value
              if (currentState != null) {
                val scope = rememberCoroutineScope()
                Test(
                  sendAction = { scope.launch { stateMachine.dispatch(it) } },
                )
              }
            }
            
        """.trimIndent()

        test(withoutSendAction, "com/test/Test.kt", source, expected)
    }
}

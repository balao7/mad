package com.freeletics.mad.whetstone.codegen.common

import com.freeletics.mad.whetstone.BaseData
import com.freeletics.mad.whetstone.ComposeFragmentData
import com.freeletics.mad.whetstone.ComposeScreenData
import com.freeletics.mad.whetstone.NavEntryData
import com.freeletics.mad.whetstone.RendererFragmentData
import com.freeletics.mad.whetstone.codegen.Generator
import com.freeletics.mad.whetstone.codegen.util.asParameter
import com.freeletics.mad.whetstone.codegen.util.bindsInstanceParameter
import com.freeletics.mad.whetstone.codegen.util.contributesToAnnotation
import com.freeletics.mad.whetstone.codegen.util.navEntryAnnotation
import com.freeletics.mad.whetstone.codegen.util.navEventNavigator
import com.freeletics.mad.whetstone.codegen.util.optInAnnotation
import com.freeletics.mad.whetstone.codegen.util.savedStateHandle
import com.freeletics.mad.whetstone.codegen.util.scopeToAnnotation
import com.freeletics.mad.whetstone.codegen.util.simplePropertySpec
import com.freeletics.mad.whetstone.codegen.util.subcomponentAnnotation
import com.freeletics.mad.whetstone.codegen.util.subcomponentFactoryAnnotation
import com.squareup.anvil.compiler.internal.decapitalize
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.GET
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import java.io.Closeable

internal val Generator<out BaseData>.retainedComponentClassName
    get() = ClassName("Whetstone${data.baseName}Component")

internal const val retainedComponentFactoryCreateName = "create"

internal val Generator<out BaseData>.retainedComponentFactoryClassName
    get() = retainedComponentClassName.nestedClass("Factory")

internal const val closeableSetPropertyName = "closeables"

internal val Generator<out BaseData>.retainedParentComponentClassName
    get() = retainedComponentClassName.nestedClass("ParentComponent")

internal val Generator<out BaseData>.retainedParentComponentGetterName
    get() = "${retainedComponentClassName.simpleName.decapitalize()}Factory"

internal class ComponentGenerator(
    override val data: BaseData,
) : Generator<BaseData>() {

    fun generate(): TypeSpec {
        return TypeSpec.interfaceBuilder(retainedComponentClassName)
            .addAnnotation(optInAnnotation())
            .addAnnotation(scopeToAnnotation(data.scope))
            .addAnnotation(subcomponentAnnotation(data.scope, data.parentScope))
            .addSuperinterface(Closeable::class)
            .addProperties(componentProperties())
            .addFunction(closeFunction())
            .addType(retainedComponentFactory())
            .addType(retainedComponentFactoryParentComponent())
            .build()
    }

    private fun componentProperties(): List<PropertySpec> {
        val properties = mutableListOf<PropertySpec>()
        if (data.stateMachine != null) {
            properties += simplePropertySpec(data.stateMachine!!)
        }
        if (data.navigation != null && data !is NavEntryData) {
            properties += simplePropertySpec(navEventNavigator)
        }
        when (data) {
            is ComposeFragmentData -> {
                properties += data.composableParameter.map {
                    PropertySpec.builder(it.name, it.typeName).build()
                }
            }
            is ComposeScreenData -> {
                properties += data.composableParameter.map {
                    PropertySpec.builder(it.name, it.typeName).build()
                }
            }
            is RendererFragmentData -> properties += simplePropertySpec(data.factory)
            is NavEntryData -> {}
        }
        properties += PropertySpec.builder(closeableSetPropertyName, SET.parameterizedBy(Closeable::class.asTypeName()))
            .apply {
                if (data is NavEntryData) {
                    addAnnotation(navEntryAnnotation(data.scope, GET))
                }
            }
            .build()
        return properties
    }

    private fun closeFunction(): FunSpec {
        return FunSpec.builder("close")
            .addModifiers(OVERRIDE)
            .beginControlFlow("%L.forEach {", closeableSetPropertyName)
            .addStatement("it.close()")
            .endControlFlow()
            .build()
    }

    private fun retainedComponentFactory(): TypeSpec {
        val qualifier = if (data is NavEntryData) {
            navEntryAnnotation(data.scope)
        } else {
            null
        }
        val createFun = FunSpec.builder(retainedComponentFactoryCreateName)
            .addModifiers(ABSTRACT)
            .addParameter(bindsInstanceParameter("savedStateHandle", savedStateHandle, qualifier))
            .addParameter(bindsInstanceParameter(data.navigation.asParameter(), qualifier))
            .returns(retainedComponentClassName)
            .build()
        return TypeSpec.interfaceBuilder(retainedComponentFactoryClassName)
            .addAnnotation(subcomponentFactoryAnnotation())
            .addFunction(createFun)
            .build()
    }

    private fun retainedComponentFactoryParentComponent(): TypeSpec {
        val getterFun = FunSpec.builder(retainedParentComponentGetterName)
            .addModifiers(ABSTRACT)
            .returns(retainedComponentFactoryClassName)
            .build()
        return TypeSpec.interfaceBuilder(retainedParentComponentClassName)
            .addAnnotation(contributesToAnnotation(data.parentScope))
            .addFunction(getterFun)
            .build()
    }
}

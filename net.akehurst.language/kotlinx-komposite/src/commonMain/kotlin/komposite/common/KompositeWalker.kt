/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.kotlinx.komposite.common

import net.akehurst.kotlinx.komposite.api.KompositeException
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib

inline fun <P : Any?, A : Any?> kompositeWalker(registry: DatatypeRegistry, init: KompositeWalker.Builder<P, A>.() -> Unit): KompositeWalker<P, A> {
    val builder = KompositeWalker.Builder<P, A>()
    builder.init()
    return builder.build(registry)
}

data class WalkInfo<P, A>(
    val up: P,
    val acc: A
)

typealias NullFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, type: TypeInstance) -> WalkInfo<P, A>
typealias EnumFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, data: Enum<*>, type: EnumType) -> WalkInfo<P, A>
typealias PrimitiveFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, data: Any, type: PrimitiveType, mapper: PrimitiveMapper<*, *>?) -> WalkInfo<P, A>
typealias ValueTypeFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, data: Any, type: ValueType) -> WalkInfo<P, A>
typealias SingletonFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, data: Any, type: SingletonType) -> WalkInfo<P, A>
typealias ReferenceFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, data: Any, property: PropertyDeclaration) -> WalkInfo<P, A>

typealias CollBeginFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, data: Collection<*>, type: CollectionType, elementType: TypeInstance) -> WalkInfo<P, A>
typealias CollElementBeginFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, element: Any?, elementType: TypeInstance) -> WalkInfo<P, A>
typealias CollElementEndFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, element: Any?, elementType: TypeInstance) -> WalkInfo<P, A>
typealias CollSeperatorFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, data: Collection<*>, type: CollectionType, previousElement: Any?, elementType: TypeInstance) -> WalkInfo<P, A>
typealias CollEndFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, data: Collection<*>, type: CollectionType, elementType: TypeInstance) -> WalkInfo<P, A>

typealias MapBeginFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, data: Map<*, *>, type: CollectionType, entryKeyType: TypeInstance, entryValType: TypeInstance) -> WalkInfo<P, A>
typealias MapEntryKeyBeginFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>, entryKeyType: TypeInstance, entryValType: TypeInstance) -> WalkInfo<P, A>
typealias MapEntryKeyEndFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>, entryKeyType: TypeInstance, entryValType: TypeInstance) -> WalkInfo<P, A>
typealias MapEntryValueBeginFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>, entryKeyType: TypeInstance, entryValType: TypeInstance) -> WalkInfo<P, A>
typealias MapEntryValueEndFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>, entryKeyType: TypeInstance, entryValType: TypeInstance) -> WalkInfo<P, A>
typealias MapSeparatorFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, data: Map<*, *>, type: CollectionType, previousEntry: Map.Entry<*, *>, entryKeyType: TypeInstance, entryValType: TypeInstance) -> WalkInfo<P, A>
typealias MapEndFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, data: Map<*, *>, type: CollectionType, entryKeyType: TypeInstance, entryValType: TypeInstance) -> WalkInfo<P, A>

typealias ObjectBeginFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, obj: Any, datatype: DataType) -> WalkInfo<P, A>
typealias ObjectEndFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, obj: Any, datatype: DataType) -> WalkInfo<P, A>
typealias PropertyBeginFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, property: PropertyDeclaration) -> WalkInfo<P, A>
typealias PropertyEndFunc<P, A> = (path: List<String>, info: WalkInfo<P, A>, property: PropertyDeclaration) -> WalkInfo<P, A>

class KompositeWalker<P : Any?, A : Any?>(
    val configuration: Configuration,
    val registry: DatatypeRegistry,
    val objectBegin: ObjectBeginFunc<P, A>,
    val objectEnd: ObjectEndFunc<P, A>,
    val propertyBegin: PropertyBeginFunc<P, A>,
    val propertyEnd: PropertyEndFunc<P, A>,

    val mapBegin: MapBeginFunc<P, A>,
    val mapEntryKeyBegin: MapEntryKeyBeginFunc<P, A>,
    val mapEntryKeyEnd: MapEntryKeyEndFunc<P, A>,
    val mapEntryValueBegin: MapEntryValueBeginFunc<P, A>,
    val mapEntryValueEnd: MapEntryValueEndFunc<P, A>,
    val mapSeparate: MapSeparatorFunc<P, A>,
    val mapEnd: MapEndFunc<P, A>,

    val collBegin: CollBeginFunc<P, A>,
    val collElementBegin: CollElementBeginFunc<P, A>,
    val collElementEnd: CollElementEndFunc<P, A>,
    val collSeparate: CollSeperatorFunc<P, A>,
    val collEnd: CollEndFunc<P, A>,

    val reference: ReferenceFunc<P, A>,
    val singleton: SingletonFunc<P, A>,
    val primitive: PrimitiveFunc<P, A>,
    val valueType: ValueTypeFunc<P, A>,
    val enum: EnumFunc<P, A>,
    val nullValue: NullFunc<P, A>
) {

    class Configuration(
        var ELEMENTS: String = "\$elements",
        var ENTRIES: String = "\$entries",
        var KEY: String = "\$key",
        var VALUE: String = "\$value"
    )

    class Builder<P : Any?, A : Any?>() {
        private var _configuration: Configuration = Configuration()

        private var _objectBegin: ObjectBeginFunc<P, A> = { _, info, _, _ -> info }
        private var _objectEnd: ObjectEndFunc<P, A> = { _, info, _, _ -> info }
        private var _propertyBegin: PropertyBeginFunc<P, A> = { _, info, _ -> info }
        private var _propertyEnd: PropertyEndFunc<P, A> = { _, info, _ -> info }

        private var _mapBegin: MapBeginFunc<P, A> = { _, info, _, _, _, _ -> info }
        private var _mapEntryKeyBegin: MapEntryKeyBeginFunc<P, A> = { _, info, _, _, _ -> info }
        private var _mapEntryKeyEnd: MapEntryKeyEndFunc<P, A> = { _, info, _, _, _ -> info }
        private var _mapEntryValueBegin: MapEntryValueBeginFunc<P, A> = { _, info, _, _, _ -> info }
        private var _mapEntryValueEnd: MapEntryValueEndFunc<P, A> = { _, info, _, _, _ -> info }
        private var _mapSeparate: MapSeparatorFunc<P, A> = { _, info, _, _, _, _, _ -> info }
        private var _mapEnd: MapEndFunc<P, A> = { _, info, _, _, _, _ -> info }

        private var _collBegin: CollBeginFunc<P, A> = { _, info, _, _, _ -> info }
        private var _collElementBegin: CollElementBeginFunc<P, A> = { _, info, _, _ -> info }
        private var _collElementEnd: CollElementEndFunc<P, A> = { _, info, _, _ -> info }
        private var _collSeparate: CollSeperatorFunc<P, A> = { _, info, _, _, _, _ -> info }
        private var _collEnd: CollEndFunc<P, A> = { _, info, _, _, _ -> info }

        private var _reference: ReferenceFunc<P, A> = { _, info, _, _ -> info }
        private var _singleton: SingletonFunc<P, A> = { _, info, _, _ -> info }
        private var _primitive: PrimitiveFunc<P, A> = { _, info, _, _, _ -> info }
        private var _valueType: ValueTypeFunc<P, A> = { _, info, _, _ -> info }
        private var _enum: EnumFunc<P, A> = { _, info, _, _ -> info }
        private var _nullValue: NullFunc<P, A> = { _, info, _ -> info }

        fun configure(configuration: Configuration.() -> Unit) {
            this._configuration.apply(configuration)
        }

        fun objectBegin(func: ObjectBeginFunc<P, A>) = run { this._objectBegin = func }
        fun objectEnd(func: ObjectEndFunc<P, A>) = run { this._objectEnd = func }
        fun propertyBegin(func: PropertyBeginFunc<P, A>) = run { this._propertyBegin = func }
        fun propertyEnd(func: PropertyEndFunc<P, A>) = run { this._propertyEnd = func }

        fun mapBegin(func: MapBeginFunc<P, A>) = run { this._mapBegin = func }
        fun mapEntryKeyBegin(func: MapEntryKeyBeginFunc<P, A>) = run { this._mapEntryKeyBegin = func }
        fun mapEntryKeyEnd(func: MapEntryKeyEndFunc<P, A>) = run { this._mapEntryKeyEnd = func }
        fun mapEntryValueBegin(func: MapEntryValueBeginFunc<P, A>) = run { this._mapEntryValueBegin = func }
        fun mapEntryValueEnd(func: MapEntryValueEndFunc<P, A>) = run { this._mapEntryValueEnd = func }
        fun mapSeparate(func: MapSeparatorFunc<P, A>) = run { this._mapSeparate = func }
        fun mapEnd(func: MapEndFunc<P, A>) = run { this._mapEnd = func }

        fun collBegin(func: CollBeginFunc<P, A>) = run { this._collBegin = func }
        fun collElementBegin(func: CollElementBeginFunc<P, A>) = run { this._collElementBegin = func }
        fun collElementEnd(func: CollElementEndFunc<P, A>) = run { this._collElementEnd = func }
        fun collSeparate(func: CollSeperatorFunc<P, A>) = run { this._collSeparate = func }
        fun collEnd(func: CollEndFunc<P, A>) = run { this._collEnd = func }

        fun reference(func: ReferenceFunc<P, A>) = run { this._reference = func }
        fun singleton(func: SingletonFunc<P, A>) = run { this._singleton = func }
        fun primitive(func: PrimitiveFunc<P, A>) = run { this._primitive = func }
        fun valueType(func: ValueTypeFunc<P, A>) = run { this._valueType = func }
        fun enum(func: EnumFunc<P, A>) = run { this._enum = func }
        fun nullValue(func: NullFunc<P, A>) = run { this._nullValue = func }

        fun build(registry: DatatypeRegistry): KompositeWalker<P, A> {
            return KompositeWalker(
                _configuration,
                registry,
                _objectBegin, _objectEnd,
                _propertyBegin, _propertyEnd,
                _mapBegin, _mapEntryKeyBegin, _mapEntryKeyEnd, _mapEntryValueBegin, _mapEntryValueEnd, _mapSeparate, _mapEnd,
                _collBegin, _collElementBegin, _collElementEnd, _collSeparate, _collEnd,
                _reference, _singleton, _primitive, _valueType, _enum, _nullValue
            )
        }
    }

    fun walk(info: WalkInfo<P, A>, data: Any?, targetType: TypeInstance? = null): WalkInfo<P, A> {
        val path = emptyList<String>()
        return walkValue(null, path, info, data, targetType)
    }

    protected fun walkValue(owningProperty: PropertyDeclaration?, path: List<String>, info: WalkInfo<P, A>, data: Any?, targetType: TypeInstance?): WalkInfo<P, A> {
        return when {
            null == data -> when {
                null == targetType -> walkNull(path, info, SimpleTypeModelStdLib.NothingType)
                else -> when {
                    targetType.isNullable -> walkNull(path, info, targetType)
                    else -> throw KompositeException("data is null but targetType is not nullable: ${targetType.signature(null, 0)}")
                }
            }

            null == targetType -> when {
                data is Array<*> -> {
                    val et = SimpleTypeModelStdLib.AnyType
                    val dt = SimpleTypeModelStdLib.List
                    val ti = dt.type(arguments = listOf(et))
                    walkValueWithType(owningProperty, path, info, data, ti)
                }

                data is List<*> -> {
                    val et = SimpleTypeModelStdLib.AnyType
                    val dt = SimpleTypeModelStdLib.List
                    val ti = dt.type(arguments = listOf(et))
                    walkValueWithType(owningProperty, path, info, data, ti)
                }

                data is Set<*> -> {
                    val et = SimpleTypeModelStdLib.AnyType
                    val dt = SimpleTypeModelStdLib.Set
                    val ti = dt.type(arguments = listOf(et))
                    walkValueWithType(owningProperty, path, info, data, ti)
                }

                data is Map<*, *> -> {
                    val et = SimpleTypeModelStdLib.AnyType
                    val dt = SimpleTypeModelStdLib.Map
                    val ti = dt.type(arguments = listOf(et, et))
                    walkValueWithType(owningProperty, path, info, data, ti)
                }

                else -> {
                    //TODO: would like to use qualifiedNAme but not suported on JS
                    val dt = registry.findFirstByNameOrNull(SimpleName(data::class.simpleName!!))
                        ?: throw KompositeException("Cannot find a targetType for data object of kclass: ${data::class.simpleName}")
                    val ti = dt.type()
                    walkValueWithType(owningProperty, path, info, data, ti)
                }
            }

            else -> walkValueWithType(owningProperty, path, info, data, targetType)
        }
    }

    protected fun walkValueWithType(owningProperty: PropertyDeclaration?, path: List<String>, info: WalkInfo<P, A>, data: Any, targetType: TypeInstance): WalkInfo<P, A> {
        val dt = targetType.declaration
        return when {
            targetType == SimpleTypeModelStdLib.AnyType -> walkValue(owningProperty, path, info, data, null) // figure out type from data
            else -> when (dt) {
                is PrimitiveType -> walkPrimitive(path, info, data, dt)
                is ValueType -> walkValueType(path, info, data, dt)
                is EnumType -> walkEnum(path, info, data, dt)
                is SingletonType -> walkSingleton(path, info, data, dt)
                is CollectionType -> walkCollection(owningProperty, path, info, data, targetType)
                is DataType -> walkObject(owningProperty, path, info, data, targetType)
                else -> throw KompositeException("Don't know how to walk object with targetType: ${dt.signature(null, 0)}")
            }
        }
    }

    protected fun walkPrimitive(path: List<String>, info: WalkInfo<P, A>, data: Any, targetType: PrimitiveType): WalkInfo<P, A> {
        val mapper = this.registry.findPrimitiveMapperByKClass(data::class)
        return this.primitive(path, info, data, targetType, mapper)
    }

    protected fun walkEnum(path: List<String>, info: WalkInfo<P, A>, data: Any, targetType: EnumType): WalkInfo<P, A> {
        //val mapper = this.registry.findPrimitiveMapperFor(primitive::class)
        return this.enum(path, info, data as Enum<*>, targetType)
    }

    protected fun walkValueType(path: List<String>, info: WalkInfo<P, A>, data: Any, targetType: ValueType): WalkInfo<P, A> {
        return this.valueType(path, info, data, targetType)
    }

    protected fun walkSingleton(path: List<String>, info: WalkInfo<P, A>, data: Any, targetType: SingletonType): WalkInfo<P, A> {
        return this.singleton(path, info, data, targetType)
    }

    protected fun walkPropertyValue(owningProperty: PropertyDeclaration, path: List<String>, info: WalkInfo<P, A>, propValue: Any?): WalkInfo<P, A> {
        // PrimitiveType, ValueType and EnumType are always walked as if they were composite - the value is the 'reference'
        val propDt = owningProperty.typeInstance.declaration
        val propValType = owningProperty.typeInstance
        return when {
            null == propValue -> walkNull(path, info, propValType)
            propDt is PrimitiveType -> walkPrimitive(path, info, propValue, propDt)
            propDt is ValueType -> walkValueType(path, info, propValue, propDt)
            propDt is EnumType -> walkEnum(path, info, propValue, propDt)
            owningProperty.isComposite -> walkValueWithType(owningProperty, path, info, propValue, propValType)
            owningProperty.isReference -> walkReference(owningProperty, path, info, propValue)
            else -> throw KompositeException("Don't know how to walk property $owningProperty = $propValue")
        }
    }

    protected fun walkObject(owningProperty: PropertyDeclaration?, path: List<String>, info: WalkInfo<P, A>, data: Any, targetType: TypeInstance): WalkInfo<P, A> {
        val dt = targetType.declaration as DataType
        val infoob = this.objectBegin(path, info, data, dt)
        var acc = infoob.acc

        //obj.reflect().allPropertyNames.forEach { propName ->
        //    val prop = dt.allExplicitProperty[propName]
        //            ?: DatatypePropertySimple(dt, propName, DatatypeModelSimple.ANY_TYPE_REF { tref -> dt.namespace.model.resolve(tref) }) //default is a reference property

        //must do composites before refs, so refs refer to composites,
        //TODO...do we need to walk the tree twice to really do this correctly?
        val compProps = dt.allProperty.values.filter { it.isComposite }
        val refProps = dt.allProperty.values.filter { it.isReference }

        compProps.forEach { prop -> acc = walkProperty(prop, path, infoob, acc, data) }
        refProps.forEach { prop -> acc = walkProperty(prop, path, infoob, acc, data) }
        return this.objectEnd(path, WalkInfo(info.up, acc), data, dt)
    }

    private fun walkProperty(prop: PropertyDeclaration, path: List<String>, infoob: WalkInfo<P, A>, acc: A, obj: Any): A {
        return if (prop.characteristics.isEmpty()) {
            // ignore it
            //TODO: log!
            acc
        } else {
            val propValue = prop.get(obj)
            val ppath = path + prop.name.value
            val infopb = this.propertyBegin(ppath, WalkInfo(infoob.up, acc), prop)
            val infowp = this.walkPropertyValue(prop, ppath, WalkInfo(infoob.up, infopb.acc), propValue)
            val infope = this.propertyEnd(ppath, WalkInfo(infoob.up, infowp.acc), prop)
            infope.acc
        }
    }

    protected fun walkCollection(owningProperty: PropertyDeclaration?, path: List<String>, info: WalkInfo<P, A>, data: Any, targetType: TypeInstance): WalkInfo<P, A> {
        val dt = targetType.declaration as CollectionType
        return when (data) {
            is Array<*> -> walkColl(owningProperty, path, info, data.toList(), targetType)
            is Set<*> -> walkColl(owningProperty, path, info, data, targetType)
            is List<*> -> walkColl(owningProperty, path, info, data, targetType)
            is Map<*, *> -> walkMap(owningProperty, path, info, data, targetType)
            else -> throw KompositeException("Don't know how to walk collection of type ${data::class.simpleName}")
        }
    }

    protected fun walkColl(owningProperty: PropertyDeclaration?, path: List<String>, info: WalkInfo<P, A>, coll: Collection<*>, targetType: TypeInstance): WalkInfo<P, A> {
        val dt = targetType.declaration as CollectionType
        val elementType = targetType.typeArguments[0]
        val infolb = this.collBegin(path, info, coll, dt, elementType)
        var acc = infolb.acc
        val path_elements = path + this.configuration.ELEMENTS
        coll.forEachIndexed { index, element ->
            val ppath = path_elements + index.toString()
            val infobEl = WalkInfo(infolb.up, acc)
            val infoElb = this.collElementBegin(ppath, infobEl, element, elementType)
            val infoElv = this.walkCollElement(owningProperty, ppath, infoElb, element, elementType)
            val infoEle = this.collElementEnd(ppath, infoElv, element, elementType)
            val infoEls = if (index < coll.size - 1) {
                val infoas = this.collSeparate(ppath, infoEle, coll, dt, element, elementType)
                WalkInfo(infolb.up, infoas.acc)
            } else {
                //last one
                WalkInfo(infolb.up, infoEle.acc)
            }
            acc = infoEls.acc
        }
        val infole = WalkInfo(infolb.up, acc)
        return this.collEnd(path, infole, coll, dt, elementType)
    }

    protected fun walkCollElement(owningProperty: PropertyDeclaration?, path: List<String>, info: WalkInfo<P, A>, value: Any?, elementType: TypeInstance): WalkInfo<P, A> {
        return when {
            null == value -> walkNull(path, info, elementType)
            //registry.isPrimitive(value) -> walkPrimitive(path, info, value)
            null == owningProperty || owningProperty.isComposite -> walkValueWithType(owningProperty, path, info, value, elementType)
            owningProperty.isReference -> walkReference(owningProperty, path, info, value)
            else -> throw KompositeException("Don't know how to walk Collection element $owningProperty[${path.last()}] = $value")
        }
    }

    protected fun walkMap(owningProperty: PropertyDeclaration?, path: List<String>, info: WalkInfo<P, A>, data: Map<*, *>, typeInstance: TypeInstance): WalkInfo<P, A> {
        val dt = typeInstance.declaration as CollectionType
        val entryKeyType = typeInstance.typeArguments[0]
        val entryValType = typeInstance.typeArguments[1]
        val infolb = this.mapBegin(path, info, data, dt, entryKeyType, entryValType)
        var acc = infolb.acc
        val path_entries = path + this.configuration.ENTRIES
        data.entries.forEachIndexed { index, entry ->
            val infobEl = WalkInfo(infolb.up, acc)
            val ppath = path_entries + index.toString()
            val ppathKey = ppath + this.configuration.KEY
            val ppathValue = ppath + this.configuration.VALUE
            val infomekb = this.mapEntryKeyBegin(ppathKey, infobEl, entry, entryKeyType, entryValType)
            val infomekv = this.walkMapEntryKey(owningProperty, ppathKey, infomekb, entry.key, entryKeyType)
            val infomeke = this.mapEntryKeyEnd(ppathKey, infomekv, entry, entryKeyType, entryValType)
            val infomevb = this.mapEntryValueBegin(ppathValue, infobEl, entry, entryKeyType, entryValType)
            val infomev = this.walkMapEntryValue(owningProperty, ppathValue, infomevb, entry.value, entryValType)
            val infomeve = this.mapEntryValueEnd(ppathValue, infomev, entry, entryKeyType, entryValType)
            val infomes = if (index < data.size - 1) {
                val infoas = this.mapSeparate(ppath, infomeve, data, dt, entry, entryKeyType, entryValType)
                WalkInfo(infolb.up, infoas.acc)
            } else {
                //last one
                WalkInfo(infolb.up, infomeve.acc)
            }
            acc = infomes.acc
        }
        val infole = WalkInfo(infolb.up, acc)
        return this.mapEnd(path, infole, data, dt, entryKeyType, entryValType)
    }

    protected fun walkMapEntryKey(owningProperty: PropertyDeclaration?, path: List<String>, info: WalkInfo<P, A>, value: Any?, entryKeyType: TypeInstance): WalkInfo<P, A> {
        //key should always be a primitive or a reference, unless owning property is null! (i.e. map is the root)...I think !
        return when {
            null == value -> walkNull(path, info, entryKeyType)
            //registry.isPrimitive(value) -> walkPrimitive(path, info, value)
            else -> walkValueWithType(owningProperty, path, info, value, entryKeyType)
        }
    }

    protected fun walkMapEntryValue(owningProperty: PropertyDeclaration?, path: List<String>, info: WalkInfo<P, A>, value: Any?, entryValType: TypeInstance): WalkInfo<P, A> {
        return when {
            null == value -> walkNull(path, info, entryValType)
            //registry.isPrimitive(value) -> walkPrimitive(path, info, value)
            null == owningProperty || owningProperty.isComposite -> walkValueWithType(owningProperty, path, info, value, entryValType)
            owningProperty.isReference -> walkReference(owningProperty, path, info, value)
            else -> throw KompositeException("Don't know how to walk Map value $owningProperty[${path.last()}] = $value")
        }
    }

    protected fun walkReference(owningProperty: PropertyDeclaration, path: List<String>, info: WalkInfo<P, A>, propValue: Any?): WalkInfo<P, A> {
        val propValType = owningProperty.typeInstance
        return when {
            null == propValue -> walkNull(path, info, propValType)
            registry.isCollection(propValue) -> walkCollection(owningProperty, path, info, propValue, propValType)
            else -> this.reference(path, info, propValue, owningProperty)
        }
    }

    protected fun walkNull(path: List<String>, info: WalkInfo<P, A>, type: TypeInstance): WalkInfo<P, A> {
        return this.nullValue(path, info, type)
    }
}
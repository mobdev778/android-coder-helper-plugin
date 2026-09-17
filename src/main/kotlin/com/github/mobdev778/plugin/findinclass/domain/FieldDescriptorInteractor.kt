package com.github.mobdev778.plugin.findinclass.domain

import com.github.mobdev778.plugin.findinclass.data.FieldPrefixTree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FieldDescriptorInteractor(
    private val prefixTree: FieldPrefixTree,
) {

    private val _fieldsFlow = MutableStateFlow<List<FieldDescriptor>>(emptyList())
    val fieldsFlow: StateFlow<List<FieldDescriptor>> = _fieldsFlow

    fun onQueryChanged(query: String) {
        _fieldsFlow.value = prefixTree.find(query)
    }
}
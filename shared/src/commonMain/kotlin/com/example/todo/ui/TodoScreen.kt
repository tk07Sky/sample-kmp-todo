package com.example.todo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.domain.Todo
import com.example.todo.domain.TodoFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 編集ダイアログの対象。null なら閉じている。
    var editing by remember { mutableStateOf<Todo?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Todo")
                        Text(
                            text = "未完了 ${state.activeCount} 件 / 全 ${state.totalCount} 件",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::clearCompleted,
                        enabled = state.completedCount > 0,
                    ) {
                        Text("完了を削除")
                    }
                },
            )
        },
        bottomBar = {
            AddTodoBar(onSubmit = { viewModel.add(it) })
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            FilterRow(
                selected = state.filter,
                onSelect = viewModel::setFilter,
            )
            HorizontalDivider()

            if (state.todos.isEmpty()) {
                EmptyState(filter = state.filter, isLoading = state.isLoading)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(state.todos, key = { it.id }) { todo ->
                        TodoRow(
                            todo = todo,
                            onToggle = { viewModel.setDone(todo.id, it) },
                            onEdit = { editing = todo },
                            onDelete = { viewModel.delete(todo.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    editing?.let { target ->
        EditTodoDialog(
            todo = target,
            onDismiss = { editing = null },
            onConfirm = { title, notes ->
                viewModel.updateContent(target.id, title, notes)
                editing = null
            },
        )
    }
}

@Composable
private fun FilterRow(
    selected: TodoFilter,
    onSelect: (TodoFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TodoFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
            )
        }
    }
}

@Composable
private fun TodoRow(
    todo: Todo,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = todo.isDone, onCheckedChange = onToggle)
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.isDone) TextDecoration.LineThrough else null,
                color = if (todo.isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (todo.notes.isNotBlank()) {
                Text(
                    text = todo.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = formatCreatedAt(todo.createdAtEpochMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(AppIcons.Delete, contentDescription = "「${todo.title}」を削除")
        }
    }
}

@Composable
private fun EmptyState(filter: TodoFilter, isLoading: Boolean) {
    val message = when {
        isLoading -> "読み込み中…"
        filter == TodoFilter.Completed -> "完了した Todo はまだありません"
        filter == TodoFilter.Active -> "未完了の Todo はありません。お疲れさまでした"
        else -> "下の入力欄から最初の Todo を追加してください"
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AddTodoBar(onSubmit: (String) -> Boolean) {
    var text by remember { mutableStateOf("") }

    // 入力欄がソフトキーボードや画面下端に隠れないように inset 分を確保する
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("やることを入力") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (onSubmit(text)) text = "" }),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = { if (onSubmit(text)) text = "" },
                enabled = text.isNotBlank(),
            ) {
                Icon(AppIcons.Add, contentDescription = "追加")
            }
        }
    }
}

@Composable
private fun EditTodoDialog(
    todo: Todo,
    onDismiss: () -> Unit,
    onConfirm: (title: String, notes: String) -> Unit,
) {
    var title by remember(todo.id) { mutableStateOf(todo.title) }
    var notes by remember(todo.id) { mutableStateOf(todo.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Todo を編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("タイトル") },
                    singleLine = true,
                    isError = title.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("メモ（任意）") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, notes) },
                enabled = title.isNotBlank(),
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

private val TodoFilter.label: String
    get() = when (this) {
        TodoFilter.All -> "すべて"
        TodoFilter.Active -> "未完了"
        TodoFilter.Completed -> "完了"
    }

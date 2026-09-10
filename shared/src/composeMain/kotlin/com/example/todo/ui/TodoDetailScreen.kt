package com.example.todo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.todo.domain.Priority
import com.example.todo.domain.Todo
import com.example.todo.domain.normalizeTags

/**
 * Todo 1 件の詳細画面。タイトル・メモに加えて期限・優先度・タグを編集する。
 *
 * 一覧と同じ [TodoViewModel] を使い、対象の 1 件だけを購読する。
 * 削除されたら（別の経路で消えた場合も含めて）一覧へ戻す。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailScreen(
    viewModel: TodoViewModel,
    todoId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val todoFlow = remember(todoId) { viewModel.observeTodo(todoId) }
    // まだ読み込めていない状態と「消えた」状態を区別するため、初期値は Unit ではなく未取得を表す null にする
    val todo by todoFlow.collectAsState(initial = null)
    val knownTags by viewModel.knownTags.collectAsState()

    var loaded by remember(todoId) { mutableStateOf(false) }
    LaunchedEffect(todo) {
        if (todo != null) loaded = true
        // 一度読めたあとに null になったのは削除されたということなので、一覧に戻す
        if (loaded && todo == null) onBack()
    }

    val current = todo
    if (current == null) {
        LoadingDetail(onBack = onBack)
        return
    }

    DetailForm(
        todo = current,
        knownTags = knownTags,
        onBack = onBack,
        onSave = { title, notes, dueAt, priority, tags ->
            if (viewModel.updateDetails(current.id, title, notes, dueAt, priority, tags)) onBack()
        },
        onDelete = {
            viewModel.delete(current.id)
            onBack()
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingDetail(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todo の詳細") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.Back, contentDescription = "一覧へ戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "読み込み中…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailForm(
    todo: Todo,
    knownTags: List<String>,
    onBack: () -> Unit,
    onSave: (String, String, Long?, Priority, List<String>) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 編集中の値は画面のローカルに持ち、保存を押したときだけリポジトリへ書く。
    // todo.id が変わったら（別の Todo を開いたら）入力を作り直す。
    var title by remember(todo.id) { mutableStateOf(todo.title) }
    var notes by remember(todo.id) { mutableStateOf(todo.notes) }
    var dueText by remember(todo.id) { mutableStateOf(todo.dueAtEpochMillis?.let(::formatDateTime) ?: "") }
    var priority by remember(todo.id) { mutableStateOf(todo.priority) }
    var tags by remember(todo.id) { mutableStateOf(todo.tags) }
    var tagDraft by remember(todo.id) { mutableStateOf("") }

    val dueAt = parseDateTimeInput(dueText)
    val dueError = dueText.isNotBlank() && dueAt == null
    val canSave = title.isNotBlank() && !dueError

    fun addTag(raw: String) {
        tags = normalizeTags(tags + raw)
        tagDraft = ""
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Todo の詳細") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.Back, contentDescription = "一覧へ戻る")
                    }
                },
                actions = {
                    IconButton(onClick = onDelete) {
                        Icon(AppIcons.Delete, contentDescription = "この Todo を削除")
                    }
                    TextButton(
                        onClick = { onSave(title, notes, dueAt, priority, tags) },
                        enabled = canSave,
                    ) {
                        Text("保存")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = dueText,
                    onValueChange = { dueText = it },
                    label = { Text("期限（任意）") },
                    placeholder = { Text("2026-09-30 18:00") },
                    singleLine = true,
                    isError = dueError,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = if (dueError) {
                        "「2026-09-30 18:00」の形で入力してください"
                    } else {
                        "空にすると期限なしになります"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dueError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("優先度", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { value ->
                        FilterChip(
                            selected = value == priority,
                            onClick = { priority = value },
                            label = { Text(value.shortLabel) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("タグ", style = MaterialTheme.typography.labelLarge)

                if (tags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = { tags = tags - tag },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(AppIcons.Close, contentDescription = "タグ「$tag」を外す")
                                },
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = tagDraft,
                        onValueChange = { tagDraft = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("タグを追加") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { if (tagDraft.isNotBlank()) addTag(tagDraft) },
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { addTag(tagDraft) },
                        enabled = tagDraft.isNotBlank(),
                    ) {
                        Text("追加")
                    }
                }

                // 他の Todo で使われているタグを候補として出す。まだ付いていないものだけ。
                val suggestions = knownTags.filterNot { it in tags }
                if (suggestions.isNotEmpty()) {
                    Text(
                        text = "使ったことのあるタグ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { tag ->
                            SuggestionChip(
                                onClick = { addTag(tag) },
                                label = { Text(tag) },
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "作成 ${formatDateTime(todo.createdAtEpochMillis)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

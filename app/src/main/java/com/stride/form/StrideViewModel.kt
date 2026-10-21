package com.stride.form

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import java.io.File

enum class Page { HOME, SETUP, CAMERA, PROCESSING, REPORT, HISTORY, LEARN }
class StrideViewModel(application: Application) : AndroidViewModel(application) {
    val store = SessionStore(application)
    var page by mutableStateOf(Page.HOME)
    var view by mutableStateOf(ViewAngle.SIDE)
    var facing by mutableStateOf(Facing.RIGHT)
    var sessions by mutableStateOf(emptyList<RunReport>())
    var report by mutableStateOf<RunReport?>(null)
    var progress by mutableFloatStateOf(0f)
    var processingLabel by mutableStateOf("Preparing your video")
    var error by mutableStateOf<String?>(null)
    var loadingReport by mutableStateOf(false)
    var activeId: String? = null
    private var job: Job? = null
    init { refresh() }
    fun refresh() { viewModelScope.launch { sessions = withContext(Dispatchers.IO) { store.list() } } }
    fun openReport(r: RunReport) {
        if (r.demo || r.frames.isNotEmpty()) { report = r; page = Page.REPORT; return }
        if (loadingReport) return
        loadingReport = true
        viewModelScope.launch {
            try { report = withContext(Dispatchers.IO) { store.load(r.id) }; page = Page.REPORT }
            catch (e: Exception) { error = "This saved report could not be loaded. ${e.message.orEmpty()}" }
            finally { loadingReport = false }
        }
    }
    fun demo() = openReport(ExampleSession.create())
    fun allocateRecording(): File {
        val id = store.newId(); activeId = id
        return store.video(id)
    }
    fun recorded(file: File) { process(file, activeId ?: file.parentFile!!.name) }
    fun importVideo(uri: Uri) {
        val id = store.newId(); activeId = id
        page = Page.PROCESSING; progress = 0f; processingLabel = "Copying your video"
        job = viewModelScope.launch {
            try {
                val destination = store.video(id)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        destination.outputStream().use { out ->
                            val buffer = ByteArray(65536); var total = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = input.read(buffer); if (n < 0) break
                                total += n; require(total <= 600L * 1024 * 1024) { "Choose a video smaller than 600 MB." }
                                out.write(buffer, 0, n)
                            }
                        }
                    } ?: error("This video could not be opened. Choose it again from your gallery.")
                }
                analyze(destination, id)
            } catch (e: CancellationException) { withContext(NonCancellable + Dispatchers.IO) { store.delete(id) }; throw e }
            catch (e: Exception) { withContext(Dispatchers.IO) { store.delete(id) }; error = e.message ?: "Unable to import this video."; page = Page.SETUP }
        }
    }
    private fun process(file: File, id: String) {
        page = Page.PROCESSING; progress = 0f
        job = viewModelScope.launch {
            try { analyze(file, id) }
            catch (e: CancellationException) { withContext(NonCancellable + Dispatchers.IO) { store.delete(id) }; throw e }
            catch (e: Exception) { withContext(Dispatchers.IO) { store.delete(id) }; error = e.message ?: "Unable to analyze this video."; page = Page.SETUP }
        }
    }
    private suspend fun analyze(file: File, id: String) {
        processingLabel = "Following every stride"
        val chosenView = view; val chosenFacing = facing
        val result = withContext(Dispatchers.Default) {
            PoseProcessor.analyze(getApplication(), file, id, chosenView, chosenFacing, store.thumbnail(id)) { value ->
                viewModelScope.launch(Dispatchers.Main) { if (activeId == id && page == Page.PROCESSING) progress = value }
            }
        }
        processingLabel = "Saving your report"
        withContext(Dispatchers.IO) { store.save(result) }
        activeId = null; refresh(); openReport(result)
    }
    fun cancelProcessing() { job?.cancel(); activeId = null; page = Page.SETUP }
    fun discardRecording() {
        activeId?.let { id -> viewModelScope.launch(Dispatchers.IO) { store.delete(id) } }
        activeId = null; page = Page.SETUP
    }
    fun delete(r: RunReport) {
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { store.delete(r.id) }; report = null; refresh(); page = Page.HISTORY }
            catch (e: Exception) { error = e.message }
        }
    }
    fun share(r: RunReport, video: Boolean = false) {
        runCatching { store.share(r, video) }.onFailure { error = it.message ?: "No sharing app is available." }
    }
}

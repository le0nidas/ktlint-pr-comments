sealed class CollectionResult

data class CollectedChanges(val files: String) : CollectionResult() {
    init {
        require(files.isNotBlank())
    }
}
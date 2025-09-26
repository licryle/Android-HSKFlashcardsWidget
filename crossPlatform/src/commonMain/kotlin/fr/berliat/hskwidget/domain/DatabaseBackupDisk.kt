package fr.berliat.hskwidget.domain

import okio.Path

interface DatabaseBackupCallbacks {
    fun onBackupFolderSet(path: Path)
    fun onBackupFolderError()
    fun onBackupFileSelected(path: Path)
    fun onBackupFileSelectionCancelled()
}

/**
 * Common interface for database backup operations.
 */
expect class DatabaseDiskBackup {
    /**
     * Prompt user to select a backup folder.
     */
    fun selectFolder()

    /**
     * Prompt user to select a backup file to restore.
     */
    fun selectBackupFile()

    /**
     * Get previously set folder or request new one.
     */
    fun getFolder()

    /**
     * Clean old backups, keeping only [maxBackups] most recent files.
     */
    suspend fun cleanOldBackups(destinationFolder: Path, maxBackups: Int)

    /**
     * Perform the backup to the given folder.
     * Returns true on success, false on failure.
     */
    suspend fun backUp(destinationFolder: Path): Boolean
}

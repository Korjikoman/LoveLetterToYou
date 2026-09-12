package com.example.myproject.Images.DTO;

public enum ImageStatus {
    UPLOADING,              // Загрузка зарегистрирована.
    STAGED,                 // Файл проверен, перенос поставлен в очередь.
    READY,                  // Файл готов к привязке.
    ATTACHED,               // Файл привязан к письму или профилю.
    DELETE_AFTER_PROMOTION, // После переноса файл нужно удалить.
    DELETE_PENDING,         // Удаление поставлено в очередь.
    DELETED,                // Физический файл удалён.
    FAILED                  // Требуется ручная проверка.
}

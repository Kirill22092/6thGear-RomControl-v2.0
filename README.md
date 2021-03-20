# RomControl v3.0
С версии v2.0 проект претерпел некоторые изменения. Данная версия нацелена на совместимость с новыми версиями Android, во всеми ограничениями и улучшениями.
## Изменения
- Обновлены библиотеки Google
- Проект частично переведен на AndroidX
- Устранены мелкие баги
- Произведена очистка ресурсов
- Проект базируется на Material Design 2.0
- Добавлена возможность установки темы в зависимости от системы
- Оптимизированы трудоемкие процессы
- Добавлена поддержка русского языка
- Исправлены ошибки слоёв
- Проект полностью переведен на Root ~~(Т.к. без него проект строится на костылях)~~
- Обновлены библиотеки [RootTools](https://github.com/Stericson/RootTools) и [RootShell](https://github.com/Stericson/RootShell)
- Приложение полностью поддерживает все новейшие версии Android
- Добавлено несколько новых функций
- Множество мелких изменений
## Сборка
1. Установить последнюю сборку Android Studio
2. Из репозитория скачать [RootShell](https://github.com/Stericson/RootShell) и скопировать в папку с проектами
3. Из репозитория скачать [RootTools](https://github.com/Stericson/RootTools) и скопировать в папку с проектами
4. В папке с RootTools открыть файл settings.gradle, заменить содержимое на это:
```
include ':RootTools'
include ':RootShell'
project(':RootShell').projectDir = new File('C:\\Users\\kiril\\AndroidStudioProjects\\RootShell')
```
c указанием своего пути до RootShell

6. В папке с RomControl v3.0 открыть файл settings.gradle и указать свой путь до RootShell и RootTools
7. Проект можно собирать

# Практика 1. Classloaders

## Мотивация

Зачем нужны класслоадеры?

- Напоминание, как это все живет без тулинга и класслоадеров (JAR-path, все в куче). Нет модульной системы! 
- Почему в JAR-path'е плохо?
  - Нет инкапсуляции (любой класс может влезть в другой класс)
    - В целом несмертельно, но очень грустно, когда ты JDK
  - Сплит-пакеты
    - Что это такое?
    - Почему возникают?
      - Нерадивые разработчики (редко)
      - Позависели от разных версий библиотеки (часто)
    - Чем грозит?
      - Если класс есть в обоих версиях, то подгружается случайный (случайный в смысле прям совсем случайный). И если вдруг у них разная логика имплементации, то жди веселых сюрпризов
  - Нет разрешения зависимостей (в т.ч. транзитивных)
  - Низкая гранулярность зависимостей
    - Нельзя позависеть от части библиотеки. Все видели какую-нибудь дичь типа CORBA в компилешене? 
  - Описание зависимостей статическое 
    - Нельзя "на лету" догрузить/обновить новый класс, что актуально для серверных приложений
    - Нельзя "на лету" выгрузить больше ненужные классы
  - Описание зависимостей неявное 
    - Нельзя делать всякие прикольные штуки типа DI
- Вообще говоря, класслоадеры это лишь одно из средств устроить некоторое подобие модульной системы. На этом живут модули OSGi, JBoss и др.
  - При этом, в некоторых (узких) кейсах этого недостаточно, поэтому в Java 9 нас ждет JPMS aka Jigsaw. Но это уже совсем другая история

Зачем нам знать про класслоадеры?
- Стандартное благородное рвение не поощрять культ Карго
- Попадете в кровавый энтерпрайз с томкатами, сервлетами, контейнерами, и .war-файлами -- узнаете :)

-----------

## Концепт

- Мы, человеки, управляемся с классами посредством символических имен
  - Когда мы хотим создать новый класс, мы придумываем ему имя и пишем .java-файл
  - Когда мы хотим его заиспользовать, мы пишем это имя
- Но для JVM классы - это просто наборы байтов, обладающих некоторой структурой
- Вот класслодаеры - это такая прослойка между физическими кусками байтов (в памяти, на диске, на сервере на другом конце света) и символическими именами
- Т.е. класслоадер должен по символическому имени выдать кусок байтов, соответствующий этому имени
- Класслоадер - это никакой не секретный механизм компилятора или JVM, это обычный класс (скринчик сюда). Его можно экстендить и сделать свой
- Каждый класс на свете загружается неким класслоадером!
  - Кроме примитивов, ну и массивов офк
  - Привести цитату из спеки

Демо 1.
  - Добудем как-нибудь класс
  - У Class есть метод getClassLoader
  - Смотрим, видим AppClassLoader
    - Лоадит classpath
    - Обычный классоадер, совсем не вру, можно посмотреть сырцы (правда, декомпилированные, но сырцы!)

Золотое правила класслоадеров: класслоадер для всех референсов внутри класса это класслоадер для энклозинг класса.

Основная фича класслоадеров - то, что они организованы в иерархию:  
  - Демо 2. 
    - Добудим AppClassLoader
    - Посмотрим на его парента - видим ExtClassLoader
      - Лоадит не очень важный кусок JDK, например, DNSNameResolver и еще всякую шнягу
      - Показать людям lib/ext
      - Показать людям как потыкаться в джарники
      - Можно даже показать, как потыкаться в байткод, но лучше не надо
      - Вобщем, там живет всякая криптография и прочая хрень
    - Посмотрим на его парента - null
    - Видать, верх иерархии?

Wait a minute... 
  - Оба эти класслоадера - обычные джава-классы
  - Кто же лодит лоадеров?
  - Демо 3. 
    - Добудем ExtClassLoader
    - Возьмем его класс
    - Возьмем класслоадер этого класса
    - Нет такового
    - Как так, обещали ведь, что все классы лоадятся класслоадерами!
    - Это еще че, а вот если глянуть класслоадер у стринги...
    
  - На самом деле, это бутстрэпный класслоадер
  - Он такой весь особенный с ног до головы и заимплмеменчен нативно
  - И спека не врет -- просто там везде отдельно оговаривается поведение бутстрэпного класслоадера
  - И кстати именно он лоадит всякие важные JDK-классы
    - А какие? String, Class, Object, etc.

    Обсуждение того, как нужно правильно делегирвоаться по этой иерархии:
    	- Сначала спрашивать себя
    		- Полезно для изолирования ресурсов. Например, пусть мы пишем какой-нибудь программный продукт, к которому предполагаются плагины (IDEA). Эти плагины пишутся левыми людьми.
    		Весьма естественно, что в какой-то момент случиться какая-нибудь хрень с версиями -- мы позависели от одной версии какой-нибудь либы (например, логгинг какой), люди позависели от другой, все это попало в общий класспас, беда.
    		Чтобы этого избежать, мы можем подгрузить джарник с плагином отдельным класслоадером, который в первую очередь ищет локально. Плагин бандлит с собой эту либу, и т.к. класслоадер ищет сначала локально, класы логгинга будут подниматься из плагина. В то же время, все остальные классы (API платформы, JDK) не найдутся в класслоадере плагина и будут делегироваться к основному класслоадеру приложения. Победа!
    		(вставить картинки какие-нибудь)

    		- Другой юз-кейс: серверные приложения и сервлеты. У нас есть один большой сервер, который крутиться 24/7 и мы иногда хотим добавлять ему какую-то функциональность (ну например, разработали новую фичу). Перезапускать сервер - это даунтайм. Но можно написать умный сервер, которому можно постучаться и сказать: "Эй, чувак, а я тебе вот принес новый кусок, догрузи-ка его себе". И он догрузит. А чтобы при этом не случилось шэдоуинга зависимостей, новый кусок опять же надо загружать эгоистичным класслоадером.

    		- Проблемы?.. Чуть позже!
    	- Сначала спрашивать родителя

## Имплементация

Рассматриваем интерфейс подробней: (скринчик)

```java
public abstract class ClassLoader {
    // Invoked by the JVM. Will call loadClass(name, false)
    public Class<?> loadClass(String name) throws ClassNotFoundException
    
    protected Class<?> loadClass(String name, boolean resolve) {
        // check if class already loaded locally via findLoadedClass
        // check if parent already loaded class via parent.loadClass
        // pass call to successors via findClass(name)
        // resolve it if necessary
    }
    
    protected Class<?> findClass(String name) throws ClassNotFoundException
}
```

- Что делает loadClass(name)?
  - Просто делегейтит вызов к loadClass(name, resolve). Это тот самый метод, который будет дергать JVM. Можно переопредлить его и все!
- Что делает loadClass(name, resolve)? (дефолтная имплементация)
  - Проверим, вдруг мы уже загрузили этот класс с помощью `findLoadedClass`
    - Можно пооверрайдить для кастомного механизма кэширования
  - Проверим, вдруг родитель уже загрузил этот класс с помощью `paren.loadClass`
  - Отдадим наследникам в `findClass`
    - По умолчанию `findClass` просто кидается
    - Самый простой способ сделать свой класслоадер
  - Если класс нашелся, то по необходимости отрезольвить его вызовом `resolveClass`
    - По умолчанию, вызывает нативную магию
    - А вообще, че это?
      - Пока что ситуация с точки зрения JVM такая: выполняется код себе и выполняется. Да, мы-то как человеки знаем, что мы в этом коде создали КЛАСС, но для JVM это никак не отлчиается как если бы мы там квиксорт писали.
      - Надо бы как-то оповестить JVM, что мы тут принесли ей новый класс.
      - Для этого используется `resolveClass`, который на самом деле должен был называться `linkClass`, но так вышло. 
        - Байткод верифицируется, т.е. то, что там нормальные опкоды, что джампы ведут не в середину инструкции, и т.д. Коля расскажет больше!
        - Класс подготавливается, т.е. создаются всякие служебные структуры (например, таблица методов), поля инициализируются неявными дефолтными значениями. Явные инициализаторы выполняются *не здесь!*
        - Резольвятся имена (теперь уже по-настоящему резольвятся), т.е. если класс содержит ссылки на другие классы, JVM сопоставляет эти имена с непосредственными кусками байтов, отождествляюемые с этими классами
-

Грабли 1:

Запускаем, видим ClassNotFoundException. Так вот же он, чо за дела???
  - Встаем брякой
  - эвалюэтим код:
    `Arrays.toString(((URLClassLoader)Application.class.getClassLoader()).getURLs())`
  - опа че, какой-то джарник
  - А че там внутри?
  - опа, не та либа!

Грабли 2:
Запускаем, видим полный треш
  - Рвем волосы, плачем, зовем маму
  - Начинаем разбираться, почему
  - встаем брякой, смотрим че за лоадеры там живут
  - оказывается, лоадеры разные
  - и это важно, т.к. идентити класса складывается из FQN и *парент-класслоадера*. А тут они разные - JVM ругается, говорит, разные классы. 
  - Сообщение об ошибке могло быть и получше, конечно...
import kotlin.properties.Delegates

// 1. кастомное исключениехахах
class InsufficientFundsException(message: String) : Exception(message)

// 2. платёжные средства (полиморфизм)
interface PaymentMethod

data class CardPayment(val last4Digits: String) : PaymentMethod
data class CryptoPayment(val walletAddress: String) : PaymentMethod

// 3. функции-расширения и инфиксная нотация
fun Double.formatAsCurrency(): String {
    // форматируем с пробелами между разрядами и 2 знаками после точки
    val formatted = "%,.2f".format(this).replace(',', ' ')
    return "$formatted NC"
}

infix fun Double.formattedAs(currency: String): String {
    val formatted = "%,.2f".format(this).replace(',', ' ')
    return "$formatted $currency"
}

// 4. dsl — построитель кошелька
class WalletBuilder {
    var owner: String? = null
    var initialBalance: Double = 0.0
}



// 5. основной класс — smart wallet
class SmartWallet private constructor(
    val owner: String?,
    initialBalance: Double
) {
    // делегат observable — следит за изменением баланса
    var balance: Double by Delegates.observable(initialBalance) { _, old, new ->
        println("Лог: Изменение баланса: $old -> $new")
    }

    // делегат lazy — формируется только при первом обращении
    val auditInfo: String by lazy {
        println(">>> Формирование финансового отчёта...")
        "Кошелёк владельца: ${owner ?: "Анонимный пользователь"}. " +
                "Текущий баланс: ${balance.formatAsCurrency()}. " +
                "Транзакций за сессию: $transactionCount."
    }







    private var transactionCount: Int = 0

    // оператор + (пополнение)
    operator fun plus(amount: Double): SmartWallet {
        require(amount > 0) { "Сумма пополнения должна быть положительной" }
        balance += amount
        transactionCount++
        return this
    }

    // оператор - (снятие)
    operator fun minus(amount: Double): SmartWallet {
        if (amount <= 0) {
            throw InsufficientFundsException("Сумма снятия должна быть положительной")
        }
        if (amount > balance) {
            throw InsufficientFundsException(
                "Недостаточно средств: попытка снять $amount, доступно $balance"
            )
        }
        balance -= amount
        transactionCount++
        return this
    }

    // обработка платежа — полиморфизм + is + as?
    fun processPayment(method: PaymentMethod, amount: Double) {
        when (method) {
            is CardPayment -> {
                val last4 = (method as? CardPayment)?.last4Digits ?: "????"
                println("Оплата картой ****$last4 на сумму ${amount.formatAsCurrency()}")
            }
            is CryptoPayment -> {
                val address = (method as? CryptoPayment)?.walletAddress ?: "unknown"
                println("Оплата криптой на адрес $address: ${amount.formatAsCurrency()}")
            }
            else -> println("Неизвестный способ оплаты")
        }
        this - amount
    }

    // безопасный вывод владельца
    fun printOwner() {
        val name = owner?.let { "Владелец: $it" } ?: "Владелец: Анонимный пользователь"
        println(name)
    }

    companion object {
        fun fromBuilder(builder: WalletBuilder): SmartWallet {
            return SmartWallet(builder.owner, builder.initialBalance)
        }
    }
}

// 6. функция dsl — createWallet
fun createWallet(block: WalletBuilder.() -> Unit): SmartWallet {
    val builder = WalletBuilder()
    builder.block()
    return SmartWallet.fromBuilder(builder)
}

// 7. main — демонстрация
fun main() {
    println("=== 1. Создание кошелька через DSL + apply + also ===")

    val wallet = createWallet {
        owner = "Иван Иванов"
        initialBalance = 1000.0
    }.apply {
        println("Кошелёк создан (apply)")
    }.also {
        println("Кошелёк готов к работе (also)")
    }

    println("\n=== 2. Пополнение через оператор + ===")
    wallet + 500.0   // должен сработать observable

    println("\n=== 3. Безопасный вывод владельца через let ===")
    wallet.owner?.let { name ->
        println("Владелец подтверждён: $name")
    } ?: println("Анонимный пользователь")

    println("\n=== 4. Попытка снять 2000.0 (ошибка) ===")
    try {
        wallet - 2000.0
    } catch (e: InsufficientFundsException) {
        println("Ошибка: ${e.message}")
    }

    println("\n=== 5. Оплата криптой + is + as? ===")
    val crypto = CryptoPayment("0xABC123DEF456")
    val method: PaymentMethod = crypto

    if (method is CryptoPayment) {
        val address = method as? CryptoPayment
        println("Адрес крипто-кошелька: ${address?.walletAddress}")
    }

    println("\n=== 6. Ленивый отчёт auditInfo ===")
    println("До обращения к auditInfo...")
    println(wallet.auditInfo)

    println("\n=== 7. Инфиксная нотация ===")
    println(750.5 formattedAs "NC")
    println(500.0 formattedAs "USD")
    println(1000.0.formatAsCurrency())
}
package Instructions

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.atomicbalance.R

class FlyingInstructions : DialogFragment() {

    private lateinit var instructionTitle: TextView
    private lateinit var instructionText: TextView
    private lateinit var continueButton: Button

    private var currentStep = 0
    private val totalSteps = 4

    // Массивы с заголовками и текстами инструкций
    private val titleResources = listOf(
        "Что такое PWR?",
        "Принцип работы",
        "Панель управления",
        ""
    )

    private val textResources = listOf(
        "PWR это — самый распространенный в мире ядерный реактор. Внутри него под высоким давлением циркулирует вода, которая выполняет две роли: охлаждает реактор и поддерживает цепную реакцию.",
        "Устройство реактора можно сравнить с системой отопления, где тепло вырабатывается в котле (активная зона) и передается через теплообменник. Вот как это работает:\n\n1. Топливо: Топливные таблетки в активной зоне выделяют тепло, как дрова в печи, но без огня.\n\n2. Первый контур (радиоактивный): Вода в этом замкнутом контуре нагревается, но не кипит из-за огромного давления. Она становится переносчиком тепла.\n\n3. Парогенератор (теплообменник): Как радиатор, он передает тепло от радиоактивной воды первого контура чистой воде второго контура, не смешивая их.\n\n4. Второй контур (не радиоактивный): Получив тепло, вода здесь превращается в пар и крутит турбину, которая и вырабатывает электричество.",
        "На панели управления у вас есть два основных инструмента:\n\n1. Стержни управления (Тормоз и Газ)\n• Что это? Поглощающие стержни, которые вы двигаете вверх и вниз.\n• Зачем? Они поглощают нейтроны и контролируют скорость цепной реакции.\n\n2. Циркуляционные насосы (Система охлаждения)\n• Что это? Насосы, которые гоняют воду через реактор.\n• Зачем? Они отводят тепло и создают отрицательную обратную связь",
        ""
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Инфлейтим макет flying_instructions.xml
        val view = inflater.inflate(R.layout.flying_instructions, container, false)
        initViews(view)
        setupClickListeners()
        showStep(currentStep)
        return view
    }

    private fun initViews(view: View) {
        instructionTitle = view.findViewById(R.id.instruction_title)
        instructionText = view.findViewById(R.id.instruction_text)
        continueButton = view.findViewById(R.id.continue_button)
    }

    private fun setupClickListeners() {
        continueButton.setOnClickListener {
            currentStep++
            if (currentStep < totalSteps - 1) {
                showStep(currentStep)
            } else if (currentStep == totalSteps - 1) {
                // Последний шаг - меняем текст кнопки на "Начать"
                showStep(currentStep)
                continueButton.text = "Начать"
            } else {
                // Четвертое нажатие - закрываем диалог
                dismiss()
                saveTutorialCompleted()
            }
        }
    }

    private fun showStep(step: Int) {
        if (step in 0 until totalSteps) {
            instructionTitle.text = titleResources[step]
            instructionText.text = textResources[step]
        }
    }

    private fun saveTutorialCompleted() {
        // Сохраняем в SharedPreferences, что инструкции были показаны
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("tutorial_completed", true).apply()
    }

    override fun onStart() {
        super.onStart()
        // Настраиваем размер диалога
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        fun newInstance(): FlyingInstructions {
            return FlyingInstructions()
        }
    }
}
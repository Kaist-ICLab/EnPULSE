package com.example.survey_test_app

import com.example.survey_test_app.storage.CouchbaseSensorStateStorage
import com.example.survey_test_app.storage.SimpleStateStorage
import com.example.survey_test_app.ui.SurveyViewModel
import kaist.iclab.tracker.listener.SamsungHealthDataInitializer
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.survey.Survey
import kaist.iclab.tracker.sensor.survey.SurveyNotificationConfig
import kaist.iclab.tracker.sensor.survey.SurveyScheduleMethod
import kaist.iclab.tracker.sensor.survey.SurveySensor
import kaist.iclab.tracker.sensor.survey.question.CheckboxQuestion
import kaist.iclab.tracker.sensor.survey.question.NumberQuestion
import kaist.iclab.tracker.sensor.survey.question.Operator
import kaist.iclab.tracker.sensor.survey.question.Option
import kaist.iclab.tracker.sensor.survey.question.QuestionTrigger
import kaist.iclab.tracker.sensor.survey.question.RadioQuestion
import kaist.iclab.tracker.sensor.survey.question.TextQuestion
import kaist.iclab.tracker.sensor.survey.question.Predicate
import kaist.iclab.tracker.sensor.survey.question.SetPredicate
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage
import kaist.iclab.tracker.storage.couchbase.CouchbaseSurveyScheduleStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val koinModule = module {
    single {
        SamsungHealthDataInitializer(context = androidContext())
    }

    single {
        CouchbaseDB(context = androidContext())
    }

    single {
        AndroidPermissionManager(context = androidContext())
    }

    // Sensors
    single {
        CouchbaseSurveyScheduleStorage(
            couchbase = get(),
            collectionName = "SurveyScheduleStorage"
        )
    }

    single {
        SurveySensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(SurveySensor.Config(
                survey = mapOf(
                    "test" to Survey(
                        scheduleMethod = SurveyScheduleMethod.ESM(
                            minInterval = TimeUnit.MINUTES.toMillis(30),
                            maxInterval = TimeUnit.MINUTES.toMillis(45),
                            startOfDay = TimeUnit.HOURS.toMillis(9),
                            endOfDay = TimeUnit.HOURS.toMillis(25),
                            numSurvey = 30,
                        ),
                        notificationConfig = SurveyNotificationConfig(
                            title = "Survey Test",
                            description = "This is a survey test",
                            icon = R.drawable.ic_launcher_foreground
                        ),
                        TextQuestion(
                            id = 1,
                            question = "Your name?",
                            isMandatory = true,
                        ),
                        NumberQuestion(
                            id = 2,
                            question = "Your age?",
                            isMandatory = false,
                        ),
                        RadioQuestion(
                            id = 3,
                            question = "How are you?",
                            isMandatory = true,
                            option = listOf(
                                Option("Good"),
                                Option("Bad"),
                                Option("Okay"),
                                Option("Other: ", allowFreeResponse = true)
                            )
                        ),
                        CheckboxQuestion(
                            id = 4,
                            question = "Choose all even number",
                            isMandatory = false,
                            option = listOf(
                                Option("1"),
                                Option("2"),
                                Option("4"),
                                Option("5")
                            ),
                            questionTrigger = listOf(
                                QuestionTrigger(
                                    predicate = Predicate.Equal(setOf(1, 2)),
                                    children = listOf(
                                        RadioQuestion(
                                            id = 5,
                                            question = "Is P = NP?",
                                            isMandatory = true,
                                            option = listOf(
                                                Option("Hell yeah"),
                                                Option("Nah")
                                            ),
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    "fixedTime" to Survey(
                        scheduleMethod = SurveyScheduleMethod.Fixed(
                            timeOfDay = listOf(TimeUnit.HOURS.toMillis(15)),
                        ),
                        notificationConfig = SurveyNotificationConfig(
                            title = "Survey Test",
                            description = "This is a fixed time survey at 3PM",
                            icon = R.drawable.ic_launcher_foreground
                        ),
                        TextQuestion(
                            id = 6,
                            question = "Testing",
                            isMandatory = true,
                        ),
                    )
                ),
            )),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = SurveySensor::class.simpleName ?: ""
            ),
            scheduleStorage = get<CouchbaseSurveyScheduleStorage>(),
        )
    }

    // Global Controller
    single {
        MyBackgroundController.ServiceNotification(
            channelId = "BackgroundControllerService",
            channelName = "TrackerTest",
            notificationId = 1,
            title = "Tracker Test App",
            description = "Background sensor controller is running",
            icon = R.drawable.ic_launcher_foreground
        )
    }

    single<StateStorage<ControllerState>>(named("controllerState")) {
        CouchbaseStateStorage(
            couchbase = get(),
            defaultVal = ControllerState(ControllerState.FLAG.DISABLED),
            clazz = ControllerState::class.java,
            collectionName = MyBackgroundController::class.simpleName ?: ""
        )
    }

    single {
        MyBackgroundController(
            context = androidContext(),
            controllerStateStorage = get(qualifier("controllerState")),
            sensors = listOf(get<SurveySensor>()),
            serviceNotification = get(),
            allowPartialSensing = true,
        )
    }

    single {
        SurveyDataReceiver(
            context = androidContext()
        )
    }

    // ViewModel
    viewModel {
        SurveyViewModel(
            backgroundController = get(),
            permissionManager = get<AndroidPermissionManager>(),
            surveyDataReceiver = get<SurveyDataReceiver>(),
            surveyScheduleStorage = get<CouchbaseSurveyScheduleStorage>(),
        )
    }
}
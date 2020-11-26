package agh.edu.pl.facelivenessdetection.controller;

import java.util.Objects;

import agh.edu.pl.facelivenessdetection.detector.FaceLivenessDetector;
import agh.edu.pl.facelivenessdetection.visuals.DetectionVisualizer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

/*
 * Class is responsible for triggering events on active detector.
 * The controller also manages threads in application, because we cannot run detecting action with
 * the UI thread.
 */
public class FaceLivenessDetectionController {

    private PublishSubject<DetectionEvent> detectionEventSubject;

    private Disposable detectionEventSubjectSubscription;

    private Disposable backgroundTaskSubscription;

    public void activateFaceLivenessDetector(FaceLivenessDetector detector,
                                             DetectionVisualizer visualizer) {
        deactivateFaceLivenessDetector();

        detectionEventSubject = PublishSubject.create();
        detectionEventSubjectSubscription = detectionEventSubject
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(subscription -> {
                    Objects.requireNonNull(detector);
                    detector.handleDetectorActivated(visualizer);
                    startBackgroundTask(detector);
                })
                .observeOn(Schedulers.computation())
                .doOnTerminate(detector::handleDetectorDeactivated)
                .subscribe(event -> detector.handleDetectionTriggered());
    }

    private void startBackgroundTask(FaceLivenessDetector detector) {
        backgroundTaskSubscription = Completable.fromRunnable(() -> backgroundTaskRunner(detector))
                .subscribeOn(Schedulers.computation())
                .subscribe();
    }

    private void backgroundTaskRunner(FaceLivenessDetector detector) {
        try {
            detector.backgroundTask();
        } catch (InterruptedException ignored) {
        }
    }

    private void stopBackgroundTask() {
        if (backgroundTaskSubscription != null && !backgroundTaskSubscription.isDisposed()) {
            backgroundTaskSubscription.dispose();
            backgroundTaskSubscription = null;
        }
    }

    public void deactivateFaceLivenessDetector() {
        if (detectionEventSubject != null && !detectionEventSubjectSubscription.isDisposed()) {
            stopBackgroundTask();
            detectionEventSubject.onComplete();
            detectionEventSubject = null;
        }
    }

    public void performFaceLivenessVerification() {
        if (detectionEventSubject != null && !detectionEventSubjectSubscription.isDisposed()) {
            detectionEventSubject.onNext(new DetectionEvent());
        }
    }

    private static final class DetectionEvent {
        public DetectionEvent() {
        }
    }
}

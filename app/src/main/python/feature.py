import librosa
import numpy as np


def convert_list(signal):
    size = signal.size()
    Signal = np.array([0.0] * size)
    for i in range(0, size):
        Signal[i] = signal.get(i)
    return Signal


def mean_mfcc(Signal, sampling_rate, n_fft, hop_length, n_mfcc, n_mels, fmin, fmax):
    mfccs = librosa.feature.mfcc(y=Signal, sr=sampling_rate, n_fft=n_fft, hop_length=hop_length,
                                 n_mfcc=n_mfcc, n_mels=n_mels, fmin=fmin, fmax=fmax)
    mean_mfccs = np.mean(mfccs.T, axis=0)
    print(mean_mfccs)

    return mean_mfccs

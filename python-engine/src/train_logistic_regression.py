
from pathlib import Path

import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.pipeline import Pipeline
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import StratifiedKFold, cross_val_predict
from sklearn.metrics import f1_score
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    classification_report,
    confusion_matrix,
    roc_auc_score,
)


# Project paths

ROOT = Path(__file__).resolve().parents[2]

DATA_FILE = (
    ROOT
    / "python-engine"
    / "data"
    / "processed"
    / "buildings_with_shakemap.csv"
)


# Load data

df = pd.read_csv(DATA_FILE)


# Define features and target

NUMERICAL_FEATURES = [
    "pga_shakemap_g",
    "total_height_m",
    "floor_area_m2",
    "column_index_pct",
    "min_wall_index_pct",
    "vs30_shakemap",
]

CATEGORICAL_FEATURES = [
    "captive_columns",
    "soft_storey",
]

FEATURES = NUMERICAL_FEATURES + CATEGORICAL_FEATURES
TARGET = "critical_damage"

X = df[FEATURES]
y = df[TARGET].map({"N": 0, "Y": 1})

if y.isna().any():
    raise ValueError("Unexpected values in critical_damage.")

print(f"Buildings: {len(df)}")
print(f"Features: {len(FEATURES)}")
print(f"Critical damage cases: {y.sum()}")


# Preprocessing

preprocessor = ColumnTransformer(
    transformers=[
        (
            "numerical",
            StandardScaler(),
            NUMERICAL_FEATURES,
        ),
        (
            "categorical",
            OneHotEncoder(
                drop="first",
                handle_unknown="ignore",
            ),
            CATEGORICAL_FEATURES,
        ),
    ]
)


# Define Logistic Regression

model = Pipeline(
    steps=[
        ("preprocessor", preprocessor),
        (
            "classifier",
            LogisticRegression(
                C=1.0,
                max_iter=2000,
                random_state=42,
            ),
        ),
    ]
)


# Five-fold stratified cross-validation

cv = StratifiedKFold(
    n_splits=5,
    shuffle=True,
    random_state=42,
)

predicted_probabilities = cross_val_predict(
    model,
    X,
    y,
    cv=cv,
    method="predict_proba",
)[:, 1]

predictions = (predicted_probabilities >= 0.3).astype(int)



# --------------------------------------------------
# 7. Evaluate multiple classification thresholds
# --------------------------------------------------

print("\n========== THRESHOLD COMPARISON ==========")

thresholds = [
    0.1, 0.2, 0.3, 0.4, 0.5,
    0.6, 0.7, 0.8, 0.9
]

results = []

for threshold in thresholds:

    predictions = (
        predicted_probabilities >= threshold
    ).astype(int)

    results.append({
        "Threshold": threshold,
        "Accuracy": accuracy_score(y, predictions),
        "Balanced Accuracy": balanced_accuracy_score(y, predictions),
        "Precision": classification_report(
            y, predictions, output_dict=True, zero_division=0
        )["1"]["precision"],
        "Recall": classification_report(
            y, predictions, output_dict=True, zero_division=0
        )["1"]["recall"],
        "F1": f1_score(y, predictions, zero_division=0),
    })

results_df = pd.DataFrame(results)

print(
    results_df.to_string(
        index=False,
        formatters={
            "Threshold": "{:.1f}".format,
            "Accuracy": "{:.3f}".format,
            "Balanced Accuracy": "{:.3f}".format,
            "Precision": "{:.3f}".format,
            "Recall": "{:.3f}".format,
            "F1": "{:.3f}".format,
        }
    )
)

print(f"\nROC-AUC: {roc_auc_score(y, predicted_probabilities):.3f}")

print("\n========== COMPLETE ==========")





# --------------------------------------------------
# MODEL EXPLANATION
# --------------------------------------------------
#
# This script trains a Logistic Regression classifier
# to estimate the probability that a building will
# suffer critical structural damage during an
# earthquake.
#
# It uses historical building characteristics and
# earthquake measurements as input features. Numerical
# features are standardised, while categorical features
# are converted into numerical representations.
#
# The model is evaluated using five-fold stratified
# cross-validation. This means each building receives
# predictions from a model trained without that
# particular building.
#
# The evaluation metrics measure different aspects
# of performance. Accuracy measures overall correctness,
# recall measures how many critically damaged buildings
# are identified, and ROC-AUC measures how well the
# model distinguishes between the two damage classes.
#
# This is an initial baseline model, not a validated
# real-world earthquake damage predictor. Its results
# will be used to guide further feature selection,
# model development and evaluation.
#
# --------------------------------------------------

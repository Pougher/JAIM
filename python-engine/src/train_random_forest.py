
from pathlib import Path

import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
from sklearn.pipeline import Pipeline
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import StratifiedKFold, cross_val_predict
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    precision_score,
    recall_score,
    f1_score,
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


#  Preprocessing

# Decision trees do not require numerical standardisation.
# Categorical variables still need numerical encoding.

preprocessor = ColumnTransformer(
    transformers=[
        (
            "numerical",
            "passthrough",
            NUMERICAL_FEATURES,
        ),
        (
            "categorical",
            OneHotEncoder(
                drop=None,
                handle_unknown="ignore",
            ),
            CATEGORICAL_FEATURES,
        ),
    ]
)


# Define Random Forest

model = Pipeline(
    steps=[
        ("preprocessor", preprocessor),
        (
            "classifier",
            RandomForestClassifier(
                n_estimators=500,
                max_depth=4,
                min_samples_leaf=5,
                max_features="sqrt",
                class_weight="balanced",
                random_state=42,
                n_jobs=-1,
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


# Evaluate multiple thresholds

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
        "Balanced Accuracy": balanced_accuracy_score(
            y, predictions
        ),
        "Precision": precision_score(
            y, predictions, zero_division=0
        ),
        "Recall": recall_score(
            y, predictions, zero_division=0
        ),
        "F1": f1_score(
            y, predictions, zero_division=0
        ),
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

print(
    f"\nROC-AUC: "
    f"{roc_auc_score(y, predicted_probabilities):.3f}"
)


# --------------------------------------------------
# 8. Evaluate at the default threshold
# --------------------------------------------------

predictions = (
    predicted_probabilities >= 0.5
).astype(int)

print("\n========== DEFAULT THRESHOLD (0.5) ==========")

print(
    f"Accuracy: {accuracy_score(y, predictions):.3f}"
)

print(
    f"Balanced accuracy: "
    f"{balanced_accuracy_score(y, predictions):.3f}"
)

print("\nConfusion matrix:")
from sklearn.metrics import confusion_matrix
print(confusion_matrix(y, predictions))

print("\n========== COMPLETE ==========")

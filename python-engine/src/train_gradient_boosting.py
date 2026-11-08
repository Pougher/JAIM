
from pathlib import Path

import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
from sklearn.pipeline import Pipeline
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.model_selection import (
    StratifiedKFold,
    cross_validate,
)
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
    "min_wall_index_pct",
    "vs30_shakemap",
]

CATEGORICAL_FEATURES = [
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
            "passthrough",
            NUMERICAL_FEATURES,
        ),
        (
            "categorical",
            OneHotEncoder(
                handle_unknown="ignore",
            ),
            CATEGORICAL_FEATURES,
        ),
    ]
)


# Five-fold stratified cross-validation

cv = StratifiedKFold(
    n_splits=5,
    shuffle=True,
    random_state=42,
)


# Gradient Boosting experiments

print("\n========== GRADIENT BOOSTING EXPERIMENTS ==========")

configurations = {
    "A": {
        "n_estimators": 50,
        "learning_rate": 0.05,
        "max_depth": 1,
    },
    "B": {
        "n_estimators": 100,
        "learning_rate": 0.05,
        "max_depth": 1,
    },
    "C": {
        "n_estimators": 100,
        "learning_rate": 0.05,
        "max_depth": 2,
    },
    "D": {
        "n_estimators": 100,
        "learning_rate": 0.1,
        "max_depth": 2,
    },
    "E": {
        "n_estimators": 200,
        "learning_rate": 0.05,
        "max_depth": 2,
    },
}

results = []

for name, parameters in configurations.items():

    model = Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            (
                "classifier",
                GradientBoostingClassifier(
                    **parameters,
                    random_state=42,
                ),
            ),
        ]
    )

    scores = cross_validate(
        model,
        X,
        y,
        cv=cv,
        scoring={
            "roc_auc": "roc_auc",
            "balanced_accuracy": "balanced_accuracy",
        },
        return_train_score=True,
    )

    results.append({
        "Configuration": name,
        "Parameters": parameters,
        "Train ROC-AUC": scores["train_roc_auc"].mean(),
        "Validation ROC-AUC": scores["test_roc_auc"].mean(),
        "ROC-AUC SD": scores["test_roc_auc"].std(),
        "Train Balanced Accuracy": scores["train_balanced_accuracy"].mean(),
        "Validation Balanced Accuracy": scores["test_balanced_accuracy"].mean(),
    })

    print(f"\nConfiguration {name}: {parameters}")
    print(
        f"Train ROC-AUC: "
        f"{scores['train_roc_auc'].mean():.3f}"
    )
    print(
        f"Validation ROC-AUC: "
        f"{scores['test_roc_auc'].mean():.3f} "
        f"+/- {scores['test_roc_auc'].std():.3f}"
    )
    print(
        f"Validation Balanced Accuracy: "
        f"{scores['test_balanced_accuracy'].mean():.3f}"
    )


# Final comparison

results_df = pd.DataFrame(results)

print("\n========== CONFIGURATION COMPARISON ==========")

print(
    results_df[
        [
            "Configuration",
            "Train ROC-AUC",
            "Validation ROC-AUC",
            "ROC-AUC SD",
            "Train Balanced Accuracy",
            "Validation Balanced Accuracy",
        ]
    ].to_string(
        index=False,
        formatters={
            "Train ROC-AUC": "{:.3f}".format,
            "Validation ROC-AUC": "{:.3f}".format,
            "ROC-AUC SD": "{:.3f}".format,
            "Train Balanced Accuracy": "{:.3f}".format,
            "Validation Balanced Accuracy": "{:.3f}".format,
        },
    )
)

print("\n========== COMPLETE ==========")


import joblib
from pathlib import Path

import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
from sklearn.pipeline import Pipeline
from sklearn.ensemble import RandomForestClassifier
from sklearn.inspection import permutation_importance
from sklearn.model_selection import (
    StratifiedKFold,
    cross_val_predict,
    cross_validate,
)
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    confusion_matrix,
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


# Permutation feature importance

print("\n========== PERMUTATION FEATURE IMPORTANCE ==========")

importance_results = []

for fold, (train_index, validation_index) in enumerate(
    cv.split(X, y), start=1
):

    X_train = X.iloc[train_index]
    X_validation = X.iloc[validation_index]

    y_train = y.iloc[train_index]
    y_validation = y.iloc[validation_index]

    model.fit(X_train, y_train)

    importance = permutation_importance(
        model,
        X_validation,
        y_validation,
        scoring="roc_auc",
        n_repeats=30,
        random_state=42,
        n_jobs=-1,
    )

    for feature, mean, std in zip(
        FEATURES,
        importance.importances_mean,
        importance.importances_std,
    ):
        importance_results.append({
            "Fold": fold,
            "Feature": feature,
            "Importance": mean,
            "Std": std,
        })

importance_df = pd.DataFrame(importance_results)

summary = (
    importance_df
    .groupby("Feature")
    .agg(
        Mean_Importance=("Importance", "mean"),
        Std_Across_Folds=("Importance", "std"),
    )
    .sort_values("Mean_Importance", ascending=False)
)

print(
    summary.to_string(
        formatters={
            "Mean_Importance": "{:.4f}".format,
            "Std_Across_Folds": "{:.4f}".format,
        }
    )
)


# Overfitting diagnostics

print("\n========== OVERFITTING DIAGNOSTICS ==========")

cv_results = cross_validate(
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

train_auc = cv_results["train_roc_auc"].mean()
validation_auc = cv_results["test_roc_auc"].mean()

train_balanced_accuracy = (
    cv_results["train_balanced_accuracy"].mean()
)

validation_balanced_accuracy = (
    cv_results["test_balanced_accuracy"].mean()
)

print(f"Training ROC-AUC: {train_auc:.3f}")
print(f"Validation ROC-AUC: {validation_auc:.3f}")
print(f"ROC-AUC gap: {train_auc - validation_auc:.3f}")

print(
    f"Training balanced accuracy: "
    f"{train_balanced_accuracy:.3f}"
)

print(
    f"Validation balanced accuracy: "
    f"{validation_balanced_accuracy:.3f}"
)

print(
    f"Balanced accuracy gap: "
    f"{train_balanced_accuracy - validation_balanced_accuracy:.3f}"
)


# Generate out-of-fold probabilities

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


# Evaluate at the default threshold

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
print(confusion_matrix(y, predictions))

print("\n========== COMPLETE ==========")

# Train final model on the complete dataset
print("\n========== SAVING FINAL MODEL ==========")

model.fit(X, y)

MODEL_DIR = ROOT / "python-engine" / "models"
MODEL_DIR.mkdir(parents=True, exist_ok=True)

MODEL_PATH = MODEL_DIR / "random_forest_pipeline.joblib"

joblib.dump(model, MODEL_PATH)

print(f"Model saved to: {MODEL_PATH}")
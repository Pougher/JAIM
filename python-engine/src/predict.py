from pathlib import Path
import math

import joblib
import pandas as pd


# Project paths

ROOT = Path(__file__).resolve().parents[2]

MODEL_PATH = (
    ROOT
    / "python-engine"
    / "models"
    / "random_forest_pipeline.joblib"
)


# Load the trained pipeline

if not MODEL_PATH.exists():
    raise FileNotFoundError(
        f"Trained model not found: {MODEL_PATH}"
    )

model = joblib.load(MODEL_PATH)

# Recover the feature names used during training.
FEATURES = list(model.feature_names_in_)

NUMERICAL_FEATURES = [
    "pga_shakemap_g",
    "total_height_m",
    "min_wall_index_pct",
    "vs30_shakemap",
]


def predict_building(building: dict) -> dict:
    """
    Predict critical earthquake damage for one building.

    Expected inputs:
        pga_shakemap_g
        total_height_m
        min_wall_index_pct
        vs30_shakemap

    Returns:
        Dictionary containing damage probability
        and predicted damage class.
    """

    # Check that all required features are supplied.
    missing = set(FEATURES) - set(building)

    if missing:
        raise ValueError(
            f"Missing building features: {sorted(missing)}"
        )

    # Extract only the features used during training.
    input_data = {
        feature: building[feature]
        for feature in FEATURES
    }

    # Validate numerical inputs.
    for feature in NUMERICAL_FEATURES:
        value = input_data[feature]

        if isinstance(value, bool):
            raise ValueError(
                f"{feature} must be numeric."
            )

        try:
            value = float(value)
        except (TypeError, ValueError):
            raise ValueError(
                f"{feature} must be numeric."
            )

        if not math.isfinite(value):
            raise ValueError(
                f"{feature} must be finite."
            )

        if value < 0:
            raise ValueError(
                f"{feature} cannot be negative."
            )

        input_data[feature] = value

    if input_data["total_height_m"] == 0:
        raise ValueError("Building height must be positive.")

    if input_data["vs30_shakemap"] == 0:
        raise ValueError("VS30 must be positive.")

    # Convert inputs into a DataFrame.
    X = pd.DataFrame(
        [input_data],
        columns=FEATURES
    )

    # Generate prediction probability.
    probabilities = model.predict_proba(X)[0]

    # Find the probability corresponding to class 1.
    critical_index = list(model.classes_).index(1)

    critical_probability = float(
        probabilities[critical_index]
    )

    # Apply the default classification threshold.
    predicted_class = (
        "Y" if critical_probability >= 0.5 else "N"
    )

    return {
        "critical_damage_probability": critical_probability,
        "predicted_class": predicted_class
    }


# Simple standalone test

if __name__ == "__main__":

    example_building = {
        "pga_shakemap_g": 0.52,
        "total_height_m": 24.0,
        "min_wall_index_pct": 0.0007,
        "vs30_shakemap": 440
    }

    result = predict_building(example_building)

    print("\n========== BUILDING PREDICTION ==========")

    print(
        f"Critical damage probability: "
        f"{result['critical_damage_probability']:.4f}"
    )

    print(
        f"Predicted class: "
        f"{result['predicted_class']}"
    )
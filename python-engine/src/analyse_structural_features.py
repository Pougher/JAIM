
from pathlib import Path

import pandas as pd
import numpy as np


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

TARGET = "critical_damage"


# Structural features

WALL_FEATURES = [
    "wall_index_ns_pct",
    "wall_index_ew_pct",
    "min_wall_index_pct",
    "priority_index_pct",
]


# 1. Wall index statistics

print("\n========== WALL INDEX STATISTICS ==========")

print(
    df[WALL_FEATURES]
    .describe()
    .T
    .round(6)
    .to_string()
)


# 2. Wall index correlation

print("\n========== WALL INDEX CORRELATIONS ==========")

print(
    df[WALL_FEATURES]
    .corr()
    .round(3)
    .to_string()
)


# 3. Compare wall indices by damage class

print("\n========== WALL INDICES BY DAMAGE CLASS ==========")

for feature in WALL_FEATURES:

    print(f"\n{feature}")

    print(
        df.groupby(TARGET)[feature]
        .agg(["mean", "median", "std"])
        .round(6)
        .to_string()
    )


# 4. Investigate directional imbalance

print("\n========== DIRECTIONAL IMBALANCE ==========")

df["wall_index_imbalance"] = (
    df["wall_index_ns_pct"] - df["wall_index_ew_pct"]
).abs()

print(
    df.groupby(TARGET)["wall_index_imbalance"]
    .agg(["mean", "median", "std"])
    .round(6)
    .to_string()
)


# 5. Investigate height relative to wall index

print("\n========== HEIGHT-TO-WALL RATIO ==========")

# Avoid division by zero.
# NaN is used where the minimum wall index is zero.

df["height_to_wall_ratio"] = np.where(
    df["min_wall_index_pct"] > 0,
    df["total_height_m"] / df["min_wall_index_pct"],
    np.nan,
)

print(
    df.groupby(TARGET)["height_to_wall_ratio"]
    .agg(["count", "mean", "median", "std"])
    .round(3)
    .to_string()
)


# 6. Investigate age and wall index interaction

print("\n========== AGE-WALL INTERACTION ==========")

df["age_wall_interaction"] = (
    df["age_years"] * df["min_wall_index_pct"]
)

print(
    df.groupby(TARGET)["age_wall_interaction"]
    .agg(["mean", "median", "std"])
    .round(6)
    .to_string()
)


# 7. Compare all candidate features

print("\n========== CANDIDATE FEATURE CORRELATIONS ==========")

CANDIDATE_FEATURES = [
    "total_height_m",
    "age_years",
    "wall_index_ns_pct",
    "wall_index_ew_pct",
    "min_wall_index_pct",
    "priority_index_pct",
    "wall_index_imbalance",
    "height_to_wall_ratio",
    "age_wall_interaction",
]

print(
    df[CANDIDATE_FEATURES]
    .corr()
    .round(3)
    .to_string()
)


# 8. Check zero wall-index values

print("\n========== ZERO WALL INDEX COUNTS ==========")

for feature in WALL_FEATURES:

    zero_count = (df[feature] == 0).sum()

    print(f"{feature}: {zero_count}")


print("\n========== INVESTIGATION COMPLETE ==========")

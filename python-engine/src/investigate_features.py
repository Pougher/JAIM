
from pathlib import Path

import pandas as pd


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


# Basic dataset information

print("\n========== DATASET OVERVIEW ==========")

print(f"Buildings: {len(df)}")
print(f"Columns: {len(df.columns)}")

print("\nColumn names:")
print(df.columns.tolist())


# Data types and missing values

print("\n========== DATA QUALITY ==========")

quality = pd.DataFrame({
    "Data Type": df.dtypes.astype(str),
    "Missing Values": df.isna().sum(),
    "Unique Values": df.nunique(),
})

print(quality.to_string())


# Numerical feature distributions

print("\n========== NUMERICAL FEATURE STATISTICS ==========")

print(
    df.describe(
        include="number"
    ).T.to_string()
)


# Categorical feature distributions

print("\n========== CATEGORICAL FEATURE DISTRIBUTIONS ==========")

categorical_columns = df.select_dtypes(
    include=["object", "category"]
).columns

for column in categorical_columns:

    print(f"\n{column}")

    print(
        df[column]
        .value_counts(dropna=False)
        .to_string()
    )


# Compare features by damage class

print("\n========== FEATURES BY DAMAGE CLASS ==========")

numerical_columns = df.select_dtypes(
    include="number"
).columns

for column in numerical_columns:

    if column == TARGET:
        continue

    print(f"\n{column}")

    print(
        df.groupby(TARGET)[column]
        .agg(["count", "mean", "median", "std", "min", "max"])
        .round(3)
        .to_string()
    )


print("\n========== INVESTIGATION COMPLETE ==========")

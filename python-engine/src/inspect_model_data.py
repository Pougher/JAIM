
from pathlib import Path
import pandas as pd

# Locate repository root
ROOT = Path(__file__).resolve().parents[2]

DATA_FILE = (
    ROOT
    / "python-engine"
    / "data"
    / "processed"
    / "buildings_with_shakemap.csv"
)

df = pd.read_csv(DATA_FILE)

print("\n========== DATASET OVERVIEW ==========")
print(f"Shape: {df.shape}")

print("\n========== COLUMN INFORMATION ==========")
print(df.dtypes)

print("\n========== MISSING VALUES ==========")
print(df.isnull().sum())

print("\n========== DAMAGE DISTRIBUTION ==========")

damage_columns = [
    col for col in df.columns
    if "damage" in col.lower()
]

for col in damage_columns:
    print(f"\n{col}:")
    print(df[col].value_counts(dropna=False))

print("\n========== PROPOSED MODEL FEATURES ==========")

features = [
    "age_years",
    "stories",
    "total_height_m",
    "floor_area_m2",
    "column_index_pct",
    "wall_index_ns_pct",
    "wall_index_ew_pct",
    "captive_columns",
    "soft_storey",
    "vs30_shakemap",
    "pga_shakemap_g",
]

for feature in features:
    if feature not in df.columns:
        print(f"{feature}: NOT FOUND")
        continue

    print(f"\n{feature}")
    print(f"  Missing: {df[feature].isna().sum()}")
    print(f"  Unique values: {df[feature].nunique()}")

    if pd.api.types.is_numeric_dtype(df[feature]):
        print(f"  Mean: {df[feature].mean()}")
        print(f"  Min: {df[feature].min()}")
        print(f"  Max: {df[feature].max()}")
    else:
        print(df[feature].value_counts(dropna=False))

print("\n========== INSPECTION COMPLETE ==========")

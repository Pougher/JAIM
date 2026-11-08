
from pathlib import Path
import pandas as pd

ROOT = Path(__file__).resolve().parents[2]

DATA_FILE = (
    ROOT
    / "python-engine"
    / "data"
    / "processed"
    / "buildings_with_shakemap.csv"
)

df = pd.read_csv(DATA_FILE)

# Inspect damage categories
print("\n========== DAMAGE LABELS ==========")

for column in [
    "structural_damage_5class",
    "structural_damage_3class",
    "critical_damage",
]:
    print(f"\n{column}")
    print(df[column].value_counts())

# Inspect potentially duplicated measurements
columns = [
    "pga_m7_8_g",
    "pga_shakemap_g",
    "pgv_m7_8_cm_s",
    "vs30_m_s",
    "vs30_shakemap",
    "floor_area_m2",
    "total_floor_area_m2",
]

print("\n========== FEATURE CORRELATIONS ==========")
print(df[columns].corr().round(3))

# Compare critical damage against selected features
print("\n========== CRITICAL DAMAGE COMPARISON ==========")

features = [
    "stories",
    "total_height_m",
    "floor_area_m2",
    "column_index_pct",
    "wall_index_ns_pct",
    "wall_index_ew_pct",
    "pga_shakemap_g",
    "vs30_shakemap",
]

for feature in features:
    print(f"\n{feature}")
    print(
        df.groupby("critical_damage")[feature]
        .agg(["mean", "median", "std"])
        .round(4)
    )

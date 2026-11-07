
from pathlib import Path

import pandas as pd


# Project paths
PROJECT_DIR = Path(__file__).resolve().parents[1]

INPUT_FILE = PROJECT_DIR / "data" / "raw" / "ACI133v3.xlsx"
OUTPUT_DIR = PROJECT_DIR / "data" / "processed"
OUTPUT_FILE = OUTPUT_DIR / "survey_buildings.csv"


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    # Row 0 contains column names; row 1 contains units.
    data = pd.read_excel(INPUT_FILE, header=1, skiprows=[2])

    # Select relevant columns using their original spreadsheet positions.
    # Using positions avoids problems with duplicate column names.
    buildings = pd.DataFrame({
        "building_id": data.iloc[:, 0],
        "age_years": data.iloc[:, 1],
        "stories": data.iloc[:, 2],
        "total_height_m": data.iloc[:, 4],
        "floor_area_m2": data.iloc[:, 5],
        "total_floor_area_m2": data.iloc[:, 6],

        "structural_damage_5class": data.iloc[:, 20],
        "structural_damage_3class": data.iloc[:, 21],
        "critical_damage": data.iloc[:, 23],
        "captive_columns": data.iloc[:, 27],
        "soft_storey": data.iloc[:, 34],

        "latitude": data.iloc[:, 36],
        "longitude": data.iloc[:, 37],
        "vs30_m_s": data.iloc[:, 38],

        "column_index_pct": data.iloc[:, 29],
        "wall_index_ns_pct": data.iloc[:, 30],
        "wall_index_ew_pct": data.iloc[:, 31],
        "min_wall_index_pct": data.iloc[:, 32],
        "priority_index_pct": data.iloc[:, 33],

        # Mw 7.8 ground motion values from the USGS ShakeMap section.
        "pga_m7_8_g": data.iloc[:, 86],
        "pgv_m7_8_cm_s": data.iloc[:, 87],
        "sa_0_3_m7_8_g": data.iloc[:, 88],
        "sa_1_0_m7_8_g": data.iloc[:, 89],
        "sa_3_0_m7_8_g": data.iloc[:, 90],
    })

    # Convert numerical columns explicitly.
    numeric_columns = [
        column for column in buildings.columns
        if column not in [
            "building_id",
            "structural_damage_5class",
            "structural_damage_3class",
            "critical_damage",
            "captive_columns",
            "soft_storey",
        ]
    ]

    for column in numeric_columns:
        buildings[column] = pd.to_numeric(
            buildings[column], errors="coerce"
        )

    # Standardise categorical values without changing their meanings.
    categorical_columns = [
        "structural_damage_5class",
        "structural_damage_3class",
        "critical_damage",
        "captive_columns",
        "soft_storey",
    ]

    for column in categorical_columns:
        buildings[column] = (
            buildings[column].astype("string").str.strip().str.upper()
        )

    # Remove records without an ID or observed damage classification.
    buildings = buildings.dropna(
        subset=["building_id", "structural_damage_5class"]
    )

    # Save the cleaned dataset.
    buildings.to_csv(OUTPUT_FILE, index=False)

    print(f"Processed {len(buildings)} buildings.")
    print(f"Saved dataset to: {OUTPUT_FILE}")

    print("\nDamage classification counts:")
    print(buildings["structural_damage_5class"].value_counts())

    print("\nMissing values:")
    print(buildings.isna().sum())


if __name__ == "__main__":
    main()

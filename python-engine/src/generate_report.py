
import json
from pathlib import Path
from collections import Counter
from statistics import mean

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import (
    SimpleDocTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
    PageBreak,
)

# --------------------------------------------------
# PATHS
# --------------------------------------------------

ROOT = Path(__file__).resolve().parents[2]

INPUT_FILE = ROOT / "eqs" / "earthquake_results.json"
OUTPUT_FILE = ROOT / "eqs" / "earthquake_report.pdf"


# --------------------------------------------------
# SEVERITY COLOURS
# --------------------------------------------------

SEVERITY = {
    1: ("Negligible", colors.HexColor("#2ECC71")),
    2: ("Slight", colors.HexColor("#F1C40F")),
    3: ("Moderate", colors.HexColor("#E67E22")),
    4: ("Severe", colors.HexColor("#E74C3C")),
    5: ("Near/Complete Destruction", colors.HexColor("#8E0000")),
}


def average(values):
    return mean(values) if values else 0


def format_value(value, suffix=""):
    if value is None:
        return "N/A"
    return f"{value}{suffix}"


# --------------------------------------------------
# LOAD DATA
# --------------------------------------------------

def load_results():
    if not INPUT_FILE.exists():
        raise FileNotFoundError(
            f"Earthquake results not found: {INPUT_FILE}"
        )

    with open(INPUT_FILE, "r", encoding="utf-8") as file:
        data = json.load(file)

    buildings = data.get("buildings", {})

    if isinstance(buildings, dict):
        buildings = list(buildings.values())

    if not buildings:
        raise ValueError("No building results found.")

    return data, buildings


# --------------------------------------------------
# REPORT GENERATION
# --------------------------------------------------

def generate_report():

    data, buildings = load_results()
    earthquake = data.get("earthquake", {})

    total = len(buildings)

    probabilities = [
        float(b["damage_probability"])
        for b in buildings
        if b.get("damage_probability") is not None
    ]

    pgas = [
        float(b["pga"])
        for b in buildings
        if b.get("pga") is not None
    ]

    vs30_values = [
        float(b["vs30"])
        for b in buildings
        if b.get("vs30") is not None
    ]

    distances = [
        float(b["distance_km"])
        for b in buildings
        if b.get("distance_km") is not None
    ]

    severity_counts = Counter(
        int(b["damage_level"])
        for b in buildings
        if b.get("damage_level") is not None
    )

    material_counts = Counter(
        b.get("material", "Unknown")
        for b in buildings
    )

    prediction_counts = Counter(
        b.get("predicted_class", "Unknown")
        for b in buildings
    )

    # --------------------------------------------------
    # DOCUMENT SETUP
    # --------------------------------------------------

    doc = SimpleDocTemplate(
        str(OUTPUT_FILE),
        pagesize=landscape(A4),
        rightMargin=15 * mm,
        leftMargin=15 * mm,
        topMargin=15 * mm,
        bottomMargin=15 * mm,
    )

    styles = getSampleStyleSheet()

    title_style = ParagraphStyle(
        "ReportTitle",
        parent=styles["Title"],
        fontSize=24,
        alignment=TA_CENTER,
        textColor=colors.HexColor("#172554"),
        spaceAfter=8,
    )

    heading_style = ParagraphStyle(
        "SectionHeading",
        parent=styles["Heading2"],
        textColor=colors.HexColor("#1D4ED8"),
        spaceBefore=12,
        spaceAfter=8,
    )

    story = []

    # --------------------------------------------------
    # TITLE
    # --------------------------------------------------

    story.append(
        Paragraph("EARTHQUAKE IMPACT ASSESSMENT", title_style)
    )

    story.append(
        Paragraph(
            "JAIM | Simulated City Damage Report",
            styles["Normal"],
        )
    )

    story.append(Spacer(1, 12))

    story.append(
        Paragraph("1. Earthquake Overview", heading_style)
    )

    metadata = [["Parameter", "Value"]]

    for key, value in earthquake.items():
        metadata.append([
            str(key).replace("_", " ").title(),
            str(value),
        ])

    if len(metadata) == 1:
        metadata.append(["Earthquake metadata", "Not available"])

    metadata_table = Table(
        metadata,
        colWidths=[65 * mm, 170 * mm],
    )

    metadata_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0),
         colors.HexColor("#172554")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("GRID", (0, 0), (-1, -1), 0.4, colors.lightgrey),
        ("PADDING", (0, 0), (-1, -1), 6),
    ]))

    story.append(metadata_table)

    # --------------------------------------------------
    # CITY-WIDE SUMMARY
    # --------------------------------------------------

    story.append(
        Paragraph("2. City-Wide Impact Summary", heading_style)
    )

    critical_count = prediction_counts.get("Y", 0)

    summary = [
        ["Metric", "Result"],
        ["Buildings assessed", str(total)],
        ["Predicted critical damage", str(critical_count)],
        ["Average damage probability",
         f"{average(probabilities) * 100:.2f}%"],
        ["Average PGA", f"{average(pgas):.4f} g"],
        ["Average VS30", f"{average(vs30_values):.1f} m/s"],
        ["Average epicentre distance",
         f"{average(distances):.3f} km"],
    ]

    summary_table = Table(
        summary,
        colWidths=[90 * mm, 65 * mm],
    )

    summary_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0),
         colors.HexColor("#172554")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("GRID", (0, 0), (-1, -1), 0.4, colors.lightgrey),
        ("PADDING", (0, 0), (-1, -1), 6),
    ]))

    story.append(summary_table)

    # --------------------------------------------------
    # SEVERITY DISTRIBUTION
    # --------------------------------------------------

    story.append(
        Paragraph("3. Damage Severity Distribution", heading_style)
    )

    severity_table_data = [
        ["Level", "Severity", "Buildings", "Percentage"]
    ]

    for level, (name, colour) in SEVERITY.items():

        count = severity_counts.get(level, 0)
        percentage = count / total * 100

        severity_table_data.append([
            str(level),
            name,
            str(count),
            f"{percentage:.1f}%",
        ])

    severity_table = Table(
        severity_table_data,
        colWidths=[25 * mm, 90 * mm, 45 * mm, 45 * mm],
    )

    severity_styles = [
        ("BACKGROUND", (0, 0), (-1, 0),
         colors.HexColor("#172554")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("GRID", (0, 0), (-1, -1), 0.4, colors.lightgrey),
        ("PADDING", (0, 0), (-1, -1), 7),
    ]

    for row, (_, colour) in enumerate(SEVERITY.values(), start=1):
        severity_styles.append(
            ("BACKGROUND", (1, row), (1, row), colour)
        )

    severity_table.setStyle(TableStyle(severity_styles))
    story.append(severity_table)

    # --------------------------------------------------
    # MATERIAL DISTRIBUTION
    # --------------------------------------------------

    story.append(
        Paragraph("4. Building Material Distribution", heading_style)
    )

    material_data = [["Material", "Buildings", "Percentage"]]

    for material, count in material_counts.most_common():
        material_data.append([
            str(material).title(),
            str(count),
            f"{count / total * 100:.1f}%",
        ])

    material_table = Table(
        material_data,
        colWidths=[80 * mm, 45 * mm, 45 * mm],
    )

    material_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0),
         colors.HexColor("#172554")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("GRID", (0, 0), (-1, -1), 0.4, colors.lightgrey),
        ("PADDING", (0, 0), (-1, -1), 6),
    ]))

    story.append(material_table)

    # --------------------------------------------------
    # INDIVIDUAL BUILDING RESULTS
    # --------------------------------------------------

    story.append(PageBreak())

    story.append(
        Paragraph("5. Individual Building Assessments", heading_style)
    )

    building_data = [[
        "ID",
        "Material",
        "Dimensions",
        "Distance (km)",
        "PGA (g)",
        "VS30 (m/s)",
        "Damage Probability",
        "Severity",
        "Prediction",
    ]]

    for index, building in enumerate(buildings):

        level = int(building.get("damage_level", 0))
        severity_name = SEVERITY.get(
            level, ("Unknown", colors.white)
        )[0]

        building_data.append([
            str(index),
            str(building.get("material", "Unknown")).title(),
            f'{building.get("width", "?")} x '
            f'{building.get("height", "?")}',
            format_value(building.get("distance_km")),
            format_value(building.get("pga")),
            format_value(building.get("vs30")),
            f'{float(building.get("damage_probability", 0)) * 100:.2f}%',
            severity_name,
            str(building.get("predicted_class", "Unknown")),
        ])

    building_table = Table(
        building_data,
        repeatRows=1,
        colWidths=[
            12 * mm, 23 * mm, 22 * mm, 24 * mm,
            20 * mm, 23 * mm, 29 * mm, 43 * mm,
            22 * mm,
        ],
    )

    building_styles = [
        ("BACKGROUND", (0, 0), (-1, 0),
         colors.HexColor("#172554")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, -1), 7),
        ("GRID", (0, 0), (-1, -1), 0.3, colors.lightgrey),
        ("PADDING", (0, 0), (-1, -1), 5),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
    ]

    for row, building in enumerate(buildings, start=1):
        level = int(building.get("damage_level", 0))
        colour = SEVERITY.get(
            level, ("Unknown", colors.white)
        )[1]

        building_styles.append(
            ("BACKGROUND", (7, row), (7, row), colour)
        )

    building_table.setStyle(TableStyle(building_styles))
    story.append(building_table)

    # --------------------------------------------------
    # DISCLAIMER
    # --------------------------------------------------

    story.append(Spacer(1, 15))

    story.append(
        Paragraph(
            "<b>Disclaimer:</b> This report presents simulated "
            "earthquake impact predictions generated by JAIM. "
            "Damage probabilities and severity classifications "
            "are model estimates, not verified structural "
            "assessments or professional engineering inspections.",
            styles["Italic"],
        )
    )

    doc.build(story)

    print(f"PDF report generated: {OUTPUT_FILE}")


if __name__ == "__main__":
    generate_report()

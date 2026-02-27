"""
Recolora una imagen base del gato según un diccionario de partes -> color RGB.

Uso:
    from recolor_cat import recolor_cat

    colores = {
        "cuerpo": (210, 140, 70),
        "bordes": (130, 75, 40),
        "manchas": (130, 75, 40),
        "ojos": (210, 170, 40),
        "orejas": (240, 185, 170),
    }
    recolor_cat("cat_base/2a.png", colores, output_path="salida.png")
"""

import os
import cv2
import numpy as np
import pandas as pd
from collections import Counter
from scipy.spatial import KDTree


# Partes del gato: nombre normalizado -> índice de grupo (0-4)
NAME_TO_GROUP = {
    "cuerpo": 0,
    "bordes": 1,
    "manchas": 2,
    "ojos": 3,
    "orejas": 4,
}

# Para cada grupo, IDs del Top-20 de colores que se reemplazan por esa parte
SIMILAR_COLORS = [
    [1],                          # 0 -> Cuerpo
    [2, 6, 9, 10, 11, 12, 15, 18],  # 1 -> Bordes
    [3, 7, 13, 14, 16, 17, 19],     # 2 -> Manchas
    [4],                          # 3 -> Ojos
    [5],                          # 4 -> Orejas
]

TOP_N = 20


def _normalize_rgb(c):
    """Asegura que el color sea una tupla de 3 int en [0,255]."""
    if hasattr(c, "__iter__") and len(c) >= 3:
        return tuple(int(np.clip(x, 0, 255)) for x in c[:3])
    raise ValueError(f"Color no válido: {c}")


def _get_palette_df(image_path, top_n=TOP_N):
    """
    Obtiene el DataFrame con los top_n colores más frecuentes de la imagen.
    Sin visualización (solo datos).
    """
    img = cv2.imread(image_path)
    if img is None:
        raise FileNotFoundError(f"No se pudo cargar la imagen: {image_path}")

    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    pixels = img.reshape(-1, 3)
    pixels_tuple = [tuple(p) for p in pixels]
    counts = Counter(pixels_tuple)
    sorted_counts = counts.most_common()

    data_list = []
    for i, (color, count) in enumerate(sorted_counts):
        if i >= top_n:
            break
        data_list.append({
            "ID": i,
            "RGB": color,
        })

    return pd.DataFrame(data_list)


def _build_mapping_rgb(df_colores, color_dict):
    """
    Construye el mapeo RGB_original -> RGB_nuevo a partir del dict de colores.
    color_dict: dict con keys normalizados (cuerpo, bordes, manchas, ojos, orejas)
                y valores (r, g, b) o secuencia de 3 números.
    """
    mapping_rgb = {}
    for part_name, new_color in color_dict.items():
        part_lower = part_name.strip().lower()
        group_idx = NAME_TO_GROUP.get(part_lower)
        if group_idx is None:
            continue
        new_rgb = _normalize_rgb(new_color)
        for color_id in SIMILAR_COLORS[group_idx]:
            row = df_colores.loc[df_colores["ID"] == color_id]
            if not row.empty:
                ref_rgb = tuple(int(x) for x in row["RGB"].values[0])
                mapping_rgb[ref_rgb] = new_rgb
    return mapping_rgb


def recolor_cat(imagen_base, color_dict, imagen_referencia=None, output_path=None):
    """
    Recolora la imagen del gato según el diccionario de partes -> color.

    Parámetros
    ----------
    imagen_base : str
        Ruta de la imagen a recolorear (ej. "cat_base/2a.png").
    color_dict : dict
        Diccionario parte -> color. Las partes pueden ser: "cuerpo", "bordes",
        "manchas", "ojos", "orejas" (sin importar mayúsculas). El color puede ser
        una tupla (R, G, B) con enteros 0-255, ej. {"ojos": (210, 170, 40), "orejas": (240, 185, 170)}.
    imagen_referencia : str, opcional
        Imagen con la que se extrae la paleta de 20 colores. Si es None, se usa
        imagen_base como referencia.
    output_path : str, opcional
        Si se indica, se guarda la imagen resultante aquí. Si es None, se
        devuelve la imagen como array numpy (BGR o BGRA).

    Devuelve
    --------
    np.ndarray o None
        Si output_path es None, devuelve la imagen (BGR/BGRA). Si se guarda, devuelve None.
    """
    ref_path = imagen_referencia if imagen_referencia is not None else imagen_base
    df_colores = _get_palette_df(ref_path, top_n=TOP_N)
    palette_rgb = np.array(df_colores["RGB"].tolist())
    tree = KDTree(palette_rgb)

    mapping_rgb = _build_mapping_rgb(df_colores, color_dict)
    if not mapping_rgb:
        raise ValueError(
            "Ninguna clave de color_dict coincide con partes conocidas. "
            "Usa alguna de: cuerpo, bordes, manchas, ojos, orejas."
        )

    img = cv2.imread(imagen_base, cv2.IMREAD_UNCHANGED)
    if img is None:
        raise FileNotFoundError(f"No se pudo cargar la imagen: {imagen_base}")

    if len(img.shape) == 3 and img.shape[2] == 4:
        bgr = img[:, :, :3]
        alpha = img[:, :, 3]
    else:
        bgr = img
        alpha = None

    rgb_img = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
    h, w, c = rgb_img.shape
    pixels_flat = rgb_img.reshape(-1, 3)

    _, indices = tree.query(pixels_flat)
    quantized_pixels = palette_rgb[indices]
    rgb_img_quantized = quantized_pixels.reshape(h, w, c)

    new_rgb_img = rgb_img_quantized.copy()
    for orig_rgb, target_rgb in mapping_rgb.items():
        mask = np.all(rgb_img_quantized == orig_rgb, axis=-1)
        new_rgb_img[mask] = target_rgb

    final_bgr = cv2.cvtColor(new_rgb_img, cv2.COLOR_RGB2BGR)

    if alpha is not None:
        final_img = np.dstack([final_bgr, alpha])
    else:
        final_img = final_bgr

    if output_path is not None:
        d = os.path.dirname(output_path)
        if d:
            os.makedirs(d, exist_ok=True)
        cv2.imwrite(output_path, final_img)
        return None
    return final_img

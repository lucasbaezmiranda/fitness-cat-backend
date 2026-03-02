import boto3
import pandas as pd
from boto3.dynamodb.conditions import Attr
from datetime import datetime

# 1. Configuración de la sesión
# Si usas perfiles locales, añade profile_name='tu-perfil'
dynamodb = boto3.resource('dynamodb', region_name='us-east-1') 
table = dynamodb.Table('user_steps')

# 2. Escaneo de la tabla
# Nota: .scan() es costoso en tablas gigantes, pero ideal para inspección rápida
response = table.scan()
items = response.get('Items', [])

# 3. Manejo de paginación (por si tienes más de 1MB de datos)
while 'LastEvaluatedKey' in response:
    response = table.scan(ExclusiveStartKey=response['LastEvaluatedKey'])
    items.extend(response.get('Items', []))

# 4. Visualización en Pandas
df = pd.DataFrame(items)

# Reordenar columnas para que el ID aparezca primero (opcional)
if not df.empty:
    cols = df.columns.tolist()
    # Cambia 'user_id' por tu Partition Key real si es distinta
    if 'user_id' in cols:
        cols.insert(0, cols.pop(cols.index('user_id')))
        df = df[cols]

print(f"Total de registros encontrados: {len(df)}")
df["time"] = df["timestamp"].apply(lambda x: datetime.fromtimestamp(int(x)).strftime('%d/%m/%Y %H:%M:%S')) # Muestra las primeras 5 filas
df.sort_values(by="timestamp", ascending=False, inplace=True)
df.to_excel("data_user_steps.xlsx", index=False)
import mysql.connector

try:
    conn = mysql.connector.connect(
        host="localhost",
        user="root",
        password="",
        database="tpv_bazar"
    )
    cursor = conn.cursor()
    cursor.execute("SELECT name, category_id FROM categories WHERE name = 'CATEGORIA_PRUEBA_MASIVA'")
    res = cursor.fetchone()
    if res:
        name, cat_id = res
        cursor.execute(f"SELECT COUNT(*) FROM products WHERE category_id = {cat_id}")
        count = cursor.fetchone()[0]
        print(f"CATEGORIA: {name}, PRODUCTOS: {count}")
    else:
        print("CATEGORIA NO ENCONTRADA")
        
    cursor.close()
    conn.close()
except Exception as e:
    print(f"ERROR: {e}")

import mysql.connector

try:
    conn = mysql.connector.connect(
        host="localhost",
        user="root",
        password="", # From db.properties
        database="tpv_bazar"
    )
    cursor = conn.cursor()
    cursor.execute("SELECT COUNT(*) FROM products")
    count = cursor.fetchone()[0]
    print(f"TOTAL PRODUCTS IN DB: {count}")
    
    cursor.execute("SELECT COUNT(*) FROM products WHERE visible = 1")
    visible_count = cursor.fetchone()[0]
    print(f"VISIBLE PRODUCTS: {visible_count}")
    
    cursor.execute("SELECT COUNT(*) FROM categories WHERE name = 'TEST_LOAD_CATEGORY'")
    cat_exists = cursor.fetchone()[0]
    print(f"TEST_LOAD_CATEGORY EXISTS: {cat_exists}")

    if cat_exists:
        cursor.execute("SELECT category_id FROM categories WHERE name = 'TEST_LOAD_CATEGORY'")
        cat_id = cursor.fetchone()[0]
        cursor.execute(f"SELECT COUNT(*) FROM products WHERE category_id = {cat_id}")
        count_in_cat = cursor.fetchone()[0]
        print(f"PRODUCTS IN TEST_LOAD_CATEGORY: {count_in_cat}")

    cursor.close()
    conn.close()
except Exception as e:
    print(f"ERROR: {e}")

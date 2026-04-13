import mysql.connector
import random
from datetime import datetime, timedelta
import time
import sys

def seed_sales():
    try:
        db = mysql.connector.connect(
            host="localhost",
            user="root",
            password="",
            database="tpv_bazar",
            autocommit=False
        )
        cursor = db.cursor()

        print("Fetching metadata...", flush=True)
        cursor.execute("""
            SELECT p.product_id, pp.price, p.name, p.sku 
            FROM products p 
            JOIN product_prices pp ON p.product_id = pp.product_id 
            WHERE pp.price_list_id = 1
        """)
        products = cursor.fetchall()
        if not products:
            print("No products with prices found.", flush=True)
            return
        
        cursor.execute("SELECT user_id FROM users LIMIT 1")
        user_res = cursor.fetchone()
        user_id = user_res[0] if user_res else 1

        cursor.execute("SELECT client_id FROM clients LIMIT 1")
        client_res = cursor.fetchone()
        client_id = client_res[0] if client_res else None

        total_sales_to_add = 71000
        batch_size = 500
        
        start_time = time.time()
        print(f"Adding {total_sales_to_add} more sales...", flush=True)

        for i in range(0, total_sales_to_add, batch_size):
            current_batch_details = []
            
            for j in range(batch_size):
                sale_num = i + j + 1
                days_ago = random.randint(0, 365)
                dt = datetime.now() - timedelta(days=days_ago, seconds=random.randint(0, 86400))
                
                num_products = random.randint(1, 4)
                chosen_products = random.sample(products, num_products)
                
                total = 0
                tax_total = 0
                temp_details = []
                for p in chosen_products:
                    p_id, p_price, p_name, p_sku = p
                    qty = random.randint(1, 3)
                    line_total = float(p_price) * qty
                    line_tax = line_total * 0.21
                    total += line_total
                    tax_total += line_tax
                    temp_details.append((p_id, qty, p_price, line_total, 21.0, line_tax, p_name, p_sku))

                payment_method = random.choice(['CASH', 'CARD', 'MIXED'])
                cash_amt = total if payment_method == 'CASH' else (total / 2 if payment_method == 'MIXED' else 0)
                card_amt = total if payment_method == 'CARD' else (total / 2 if payment_method == 'MIXED' else 0)

                cursor.execute("""
                    INSERT INTO sales (sale_datetime, user_id, client_id, total, payment_method, iva, total_net, total_tax, cash_amount, card_amount, observations)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """, (dt.strftime('%Y-%m-%d %H:%M:%S'), user_id, client_id, total, payment_method, tax_total, total, tax_total, cash_amt, card_amt, f"Stress test sale extension #{sale_num}"))
                
                sale_id = cursor.lastrowid
                for d in temp_details:
                    current_batch_details.append((sale_id, d[0], d[1], d[2], d[3], d[4], d[5], d[6], d[7]))

            cursor.executemany("""
                INSERT INTO sale_details (sale_id, product_id, quantity, unit_price, line_total, iva_rate, iva_amount, product_name_snapshot, sku_snapshot)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, current_batch_details)

            db.commit()
            
            if (i + batch_size) % 5000 == 0:
                elapsed = time.time() - start_time
                print(f"Processed {i + batch_size}/{total_sales_to_add}... ({elapsed:.2f}s)", flush=True)

        print(f"DONE! Total time: {time.time() - start_time:.2f}s", flush=True)
        db.close()
    except Exception as e:
        print(f"ERROR: {e}", flush=True)

if __name__ == "__main__":
    seed_sales()

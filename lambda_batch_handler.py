import json
import time
import boto3

dynamodb = boto3.resource("dynamodb")
table = dynamodb.Table("user_steps")

def lambda_handler(event, context):
    """
    Handles both batch and individual step records from Android app.
    
    Batch format:
    {
        "user_id": "abc123def456_1234567890",
        "records": [
            {"steps_at_time": 100, "timestamp": 1704123456},
            {"steps_at_time": 250, "timestamp": 1704125256},
            ...
        ]
    }
    
    Individual format:
    {
        "user_id": "abc123def456_1234567890",
        "steps": 1234,
        "timestamp": 1704123456
    }
    """
    try:
        body = json.loads(event.get("body", "{}"))
        
        user_id = body.get("user_id")
        if not user_id:
            return {
                "statusCode": 400,
                "body": json.dumps({"ok": False, "error": "user_id is required"})
            }
        
        # Determine if this is batch or individual sync
        records = []
        if "records" in body:
            # Batch sync: process array of records
            for record in body["records"]:
                # Handle both "steps_at_time" (from batch) and "steps" (fallback)
                steps = record.get("steps_at_time") or record.get("steps", 0)
                timestamp = record.get("timestamp")
                records.append({"steps": steps, "timestamp": timestamp})
        elif "steps" in body:
            # Individual sync: create single record
            records = [{"steps": body["steps"], "timestamp": body.get("timestamp")}]
        else:
            return {
                "statusCode": 400,
                "body": json.dumps({"ok": False, "error": "No records or steps provided"})
            }
        
        if not records:
            return {
                "statusCode": 400,
                "body": json.dumps({"ok": False, "error": "No records to process"})
            }
        
        # Use batch_writer for better performance
        written_count = 0
        with table.batch_writer() as batch:
            for i, record in enumerate(records):
                # Handle timestamp: convert to seconds if needed, ensure uniqueness
                ts_base = record.get("timestamp")
                if not ts_base:
                    ts_base = int(time.time())  # Current time in seconds
                else:
                    ts_base = int(ts_base)
                    # If timestamp is in milliseconds (> 10 digits), convert to seconds
                    if ts_base > 1e10:
                        ts_base = ts_base // 1000
                
                # Add index to ensure uniqueness (important if multiple records have same timestamp)
                unique_ts = ts_base + i
                
                batch.put_item(
                    Item={
                        "user_id": str(user_id),
                        "timestamp": unique_ts,
                        "steps": int(record.get("steps", 0))
                    }
                )
                written_count += 1
        
        return {
            "statusCode": 200,
            "body": json.dumps({
                "ok": True,
                "count": written_count
            })
        }
        
    except KeyError as e:
        return {
            "statusCode": 400,
            "body": json.dumps({"ok": False, "error": f"Missing required field: {str(e)}"})
        }
    except Exception as e:
        print(f"Error: {str(e)}")
        import traceback
        traceback.print_exc()
        return {
            "statusCode": 500,
            "body": json.dumps({"ok": False, "error": str(e)})
        }





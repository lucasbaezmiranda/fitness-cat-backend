import json
import time
import boto3

dynamodb = boto3.resource("dynamodb")
table = dynamodb.Table("user_steps")

def lambda_handler(event, context):
    """
    Handles batch step records from Android app.
    
    Expected format:
    {
        "user_id": "abc123def456_1234567890",
        "records": [
            {"steps_at_time": 100, "timestamp": 1704123456},
            {"steps_at_time": 250, "timestamp": 1704125256},
            ...
        ]
    }
    
    Writes each record to DynamoDB as if the user sent them individually.
    """
    try:
        body = json.loads(event["body"])
        
        user_id = body["user_id"]
        records = body.get("records", [])
        
        if not records:
            return {
                "statusCode": 400,
                "body": json.dumps({"ok": False, "error": "No records provided"})
            }
        
        # Write each record to DynamoDB
        written_count = 0
        for record in records:
            item = {
                "user_id": user_id,
                "timestamp": record.get("timestamp", int(time.time())),
                "steps": record.get("steps_at_time", 0)  # Map steps_at_time to steps
            }
            
            table.put_item(Item=item)
            written_count += 1
        
        return {
            "statusCode": 200,
            "body": json.dumps({
                "ok": True,
                "written": written_count
            })
        }
        
    except KeyError as e:
        return {
            "statusCode": 400,
            "body": json.dumps({"ok": False, "error": f"Missing required field: {str(e)}"})
        }
    except Exception as e:
        return {
            "statusCode": 500,
            "body": json.dumps({"ok": False, "error": str(e)})
        }


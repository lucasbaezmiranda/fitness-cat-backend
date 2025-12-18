#!/usr/bin/env python3
"""
Script to create the DynamoDB table for the Step Tracker app.
Run this once before using the app.

Requirements:
    pip install boto3

Usage:
    python dynamodb_setup.py --region us-east-1
"""

import argparse
import boto3
from botocore.exceptions import ClientError


def create_table(region='us-east-1', table_name='StepRecords'):
    """
    Create the DynamoDB table for step records.
    
    Schema:
    - Partition Key: userId (String)
    - Sort Key: timestamp (Number)
    - Attributes: steps (Number), date (String)
    """
    dynamodb = boto3.client('dynamodb', region_name=region)
    
    try:
        response = dynamodb.create_table(
            TableName=table_name,
            KeySchema=[
                {
                    'AttributeName': 'userId',
                    'KeyType': 'HASH'  # Partition key
                },
                {
                    'AttributeName': 'timestamp',
                    'KeyType': 'RANGE'  # Sort key
                }
            ],
            AttributeDefinitions=[
                {
                    'AttributeName': 'userId',
                    'AttributeType': 'S'  # String
                },
                {
                    'AttributeName': 'timestamp',
                    'AttributeType': 'N'  # Number
                }
            ],
            BillingMode='PAY_PER_REQUEST'  # On-demand pricing (no capacity planning needed)
        )
        
        print(f"✓ Creating table '{table_name}'...")
        print(f"  Waiting for table to be active...")
        
        # Wait for table to be created
        waiter = dynamodb.get_waiter('table_exists')
        waiter.wait(TableName=table_name)
        
        print(f"✓ Table '{table_name}' created successfully!")
        print(f"  Region: {region}")
        print(f"  Partition Key: userId (String)")
        print(f"  Sort Key: timestamp (Number)")
        
        return True
        
    except ClientError as e:
        error_code = e.response['Error']['Code']
        if error_code == 'ResourceInUseException':
            print(f"⚠ Table '{table_name}' already exists.")
            return True
        else:
            print(f"✗ Error creating table: {e}")
            return False
    except Exception as e:
        print(f"✗ Unexpected error: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(description='Create DynamoDB table for Step Tracker app')
    parser.add_argument(
        '--region',
        default='us-east-1',
        help='AWS region (default: us-east-1)'
    )
    parser.add_argument(
        '--table-name',
        default='StepRecords',
        help='DynamoDB table name (default: StepRecords)'
    )
    
    args = parser.parse_args()
    
    print("Setting up DynamoDB table for Fitness Cat Step Tracker")
    print("=" * 60)
    
    # Check AWS credentials
    try:
        session = boto3.Session()
        credentials = session.get_credentials()
        if credentials is None:
            print("✗ AWS credentials not found!")
            print("  Please configure AWS credentials using one of:")
            print("    - aws configure")
            print("    - Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)")
            print("    - IAM role (if running on EC2)")
            return
    except Exception as e:
        print(f"✗ Error checking AWS credentials: {e}")
        return
    
    print("✓ AWS credentials found")
    print()
    
    # Create table
    success = create_table(region=args.region, table_name=args.table_name)
    
    if success:
        print()
        print("Setup complete! You can now use the Android app to sync step data.")
    else:
        print()
        print("Setup failed. Please check the error messages above.")


if __name__ == '__main__':
    main()


# Python Environment Setup

This project includes a Python script (`dynamodb_setup.py`) for setting up the AWS DynamoDB table. To use it, you'll need a Python virtual environment.

## Option 1: Using python3-venv (Recommended for Debian/Ubuntu)

```bash
# Install python3-venv if needed
sudo apt install python3-venv

# Create virtual environment
python3 -m venv venv

# Activate virtual environment
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Run the setup script
python dynamodb_setup.py --region us-east-1
```

## Option 2: Using virtualenv

If you have `virtualenv` installed:

```bash
# Create virtual environment
virtualenv venv

# Activate virtual environment
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Run the setup script
python dynamodb_setup.py --region us-east-1
```

## Activating the Environment

After creating the virtual environment, activate it with:
```bash
source venv/bin/activate
```

You'll see `(venv)` in your terminal prompt when it's active.

To deactivate:
```bash
deactivate
```

## Dependencies

The `requirements.txt` file includes:
- `boto3` - AWS SDK for Python (used for DynamoDB operations)


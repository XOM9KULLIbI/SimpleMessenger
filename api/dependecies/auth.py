from datetime import datetime, timedelta
from fastapi import HTTPException
from fastapi.params import Depends
from fastapi.security import OAuth2PasswordBearer
from jwt import ExpiredSignatureError, InvalidTokenError
from passlib.context import CryptContext
import jwt
from starlette import status
from api.db.queries import ORM
from api.schemas.user_schemas import User
from api.settings import key
import logging

SECRET_KEY = key
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30
REFRESH_TOKEN_EXPIRE_DAYS = 30
oauth2_schema = OAuth2PasswordBearer(tokenUrl="/token")

logger = logging.getLogger(__name__)
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)

credentials_exception = HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )

def get_password_hashed(password: str) -> str:
    return pwd_context.hash(password)

def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)

def create_access_token(data: dict) -> str:
    to_encode = data.copy()
    expire = datetime.now() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, ALGORITHM)
    return encoded_jwt

def create_refresh_token(data: dict):
    to_encode = data.copy()
    expire = datetime.now() + timedelta(days=REFRESH_TOKEN_EXPIRE_DAYS)
    to_encode.update({"exp": expire, "type": "refresh"})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, ALGORITHM)
    return encoded_jwt


def verify_access_token(token: str) -> str:
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        username = payload.get("username")
        if username is None:
            raise credentials_exception
        return username
    except ExpiredSignatureError as e:
        raise HTTPException(status_code=401, detail="Token expired")
    except InvalidTokenError as e:
        raise credentials_exception

def verify_refresh_token(token:str) -> str:
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        username: str = payload.get("username")
        token_type: str = payload.get("type")
        if username is None or token_type != "refresh":
            raise credentials_exception
        return username
    except ExpiredSignatureError as e:
        raise HTTPException(status_code=401, detail="Token expired")
    except InvalidTokenError as e:
        raise credentials_exception

async def get_current_user(token: str = Depends(oauth2_schema)) -> User:
    username = verify_access_token(token)
    user = await ORM.get_user_by_username(username)
    if user is None:
        raise credentials_exception
    return User.model_validate(user)

async def get_current_active_user(current_user: User = Depends(get_current_user)) -> User:
    if current_user.is_disabled:
        raise HTTPException(status_code=400, detail="Inactive user")
    return current_user

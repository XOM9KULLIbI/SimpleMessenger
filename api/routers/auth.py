from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import OAuth2PasswordRequestForm
from starlette import status

from db.queries import ORM
from dependecies.auth import verify_password, create_access_token, verify_refresh_token, credentials_exception
from schemas.token_schema import Token, TokenData

auth_router = APIRouter(prefix="/auth", tags=["auth"])

@auth_router.post("/token")
async def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends()):
    user = await ORM.get_user_by_username(form_data.username)
    if not user or not verify_password(form_data.password, user["hashed_password"]):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Incorrect username or password",
                            headers={"WWW-Authenticate": "Bearer"})
    access_token = create_access_token(data={"username": user["username"]})
    # refresh_token = create_refresh_token(data={"username": user["username"]})
    return Token(access_token=access_token, token_type="bearer")

@auth_router.post("/token/refresh", response_model=Token)
async def refresh_access_token(refresh_token: TokenData):
    username = verify_refresh_token(refresh_token.token)
    user = await ORM.get_user_by_username(username)
    if user is None:
        raise credentials_exception

    new_access_token = create_access_token(
        data={"username": user["username"]})

    return Token(token_type="bearer", access_token=new_access_token)
from typing import Annotated

from fastapi import APIRouter, HTTPException, Depends
from pydantic import PositiveInt
from starlette import status

from api.db.queries import ORM
from api.dependecies.auth import get_password_hashed, get_current_active_user
from api.schemas.user_schemas import UserCreate, RegisterUser, User

user_router = APIRouter(prefix="/users", tags=["users"])

@user_router.post("/")
async def register_user(user: UserCreate):
    existing_user = await ORM.get_user_by_username(user.username)
    if existing_user:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Username already registered")
    hashed_password = get_password_hashed(user.password)
    user_data = RegisterUser(**user.model_dump(), hashed_password=hashed_password).model_dump()
    created_user = await ORM.register_user(user_data)
    return created_user

@user_router.get("/me")
async def get_my_user_info(user: Annotated[User, Depends(get_current_active_user)]):
    return user

@user_router.get("/{user_id}")
async def get_user_info(user_id: PositiveInt, current_user: Annotated[User, Depends(get_current_active_user)]):
    user_data = await ORM.get_user_by_user_id(user_id)
    return User(**user_data)